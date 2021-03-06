package uk.ac.wellcome.platform.idminter.database

import java.sql.{BatchUpdateException, Statement}

import grizzled.slf4j.Logging
import scalikejdbc._
import uk.ac.wellcome.models.work.internal.SourceIdentifier
import uk.ac.wellcome.platform.idminter.models.{Identifier, IdentifiersTable}

import scala.concurrent.blocking
import scala.util.Try

object IdentifiersDao {
  case class LookupResult(
    existingIdentifiers: Map[SourceIdentifier, Identifier],
    unmintedIdentifiers: List[SourceIdentifier])
  case class InsertResult(succeeded: List[Identifier])

  case class InsertError(failed: List[Identifier],
                         e: Throwable,
                         succeeded: List[Identifier])
      extends Exception
}

class IdentifiersDao(identifiers: IdentifiersTable) extends Logging {
  import IdentifiersDao._

  def lookupIds(sourceIdentifiers: Seq[SourceIdentifier])(
    implicit session: DBSession = readOnlySession): Try[LookupResult] =
    Try {
      debug(s"Matching ($sourceIdentifiers)")
      val distinctIdentifiers = sourceIdentifiers.distinct
      val identifierRowsMap: Map[(String, String, String), SourceIdentifier] =
        distinctIdentifiers.map { id =>
          (id.ontologyType, id.identifierType.id, id.value) -> id
        }.toMap

      val foundIdentifiers =
        withTimeWarning(thresholdMillis = 10000L, distinctIdentifiers) {
          batchSourceIdentifiers(distinctIdentifiers)
            .flatMap { identifierBatch =>
              blocking {
                val i = identifiers.i
                val matchRow = rowMatcherFromSyntaxProvider(i) _

                // The query is manually constructed like this because using WHERE IN
                // statements with multiple items causes a full-index scan on MySQL 5.6.
                // See: https://stackoverflow.com/a/53180813
                // Therefore, we perform the rewrites here on the client.
                //
                // Because of issues in `scalikejdbc`'s typing, it's necessary to
                // specify the `Nothing` type parameters as done here in order for
                // the `map` statement to work properly.
                val query = withSQL[Nothing] {
                  select
                    .from[Nothing](identifiers as i)
                    .where
                    .map { query =>
                      identifierBatch.tail.foldLeft(
                        query.withRoundBracket(matchRow(identifierBatch.head))
                      ) { (q, sourceIdentifier) =>
                        q.or.withRoundBracket(matchRow(sourceIdentifier))
                      }
                    }
                }.map(rs => {
                    val row = getRowFromResult(i, rs)
                    identifierRowsMap.get(row) match {
                      case Some(sourceIdentifier) =>
                        (sourceIdentifier, Identifier(i)(rs))
                      case None =>
                        // this should be impossible in practice
                        throw new RuntimeException(
                          s"The row $row returned by the query could not be matched to a sourceIdentifier")
                    }

                  })
                  .list
                debug(s"Executing:'${query.statement}'")
                query.apply()
              }
            }
        }
      val existingIdentifiers = foundIdentifiers.toMap
      val otherIdentifiers = distinctIdentifiers.toSet -- existingIdentifiers.keySet
      LookupResult(
        existingIdentifiers = existingIdentifiers,
        unmintedIdentifiers = otherIdentifiers.toList
      )
    }

  @throws(classOf[InsertError])
  def saveIdentifiers(ids: List[Identifier])(
    implicit session: DBSession = NamedAutoSession('primary)
  ): Try[InsertResult] =
    Try {
      val values = ids.map(i =>
        Seq(i.CanonicalId, i.OntologyType, i.SourceSystem, i.SourceId))
      blocking {
        debug(s"Putting new identifier $ids")
        withSQL {
          insert
            .into(identifiers)
            .namedValues(
              identifiers.column.CanonicalId -> sqls.?,
              identifiers.column.OntologyType -> sqls.?,
              identifiers.column.SourceSystem -> sqls.?,
              identifiers.column.SourceId -> sqls.?
            )
        }.batch(values: _*).apply()
        InsertResult(ids)
      }
    } recover {
      case e: BatchUpdateException =>
        val insertError = getInsertErrorFromException(e, ids)
        val failedIds = insertError.failed.map(_.SourceId)
        val succeededIds = insertError.succeeded.map(_.SourceId)
        error(
          s"Batch update failed for [$failedIds], succeeded for [$succeededIds]",
          e)
        throw insertError
      case e =>
        error(s"Failed inserting IDs: [${ids.map(_.SourceId)}]")
        throw e
    }

  private def getInsertErrorFromException(
    exception: BatchUpdateException,
    ids: List[Identifier]): InsertError = {
    val groupedResult = ids.zip(exception.getUpdateCounts).groupBy {
      case (_, Statement.EXECUTE_FAILED) => Statement.EXECUTE_FAILED
      case (_, _)                        => Statement.SUCCESS_NO_INFO
    }
    val failedIdentifiers =
      groupedResult.getOrElse(Statement.EXECUTE_FAILED, Nil).map {
        case (identifier, _) => identifier
      }
    val succeededIdentifiers =
      groupedResult.getOrElse(Statement.SUCCESS_NO_INFO, Nil).map {
        case (identifier, _) => identifier
      }
    InsertError(failedIdentifiers, exception, succeededIdentifiers)
  }

  private def rowMatcherFromSyntaxProvider(i: SyntaxProvider[Identifier])(
    sourceIdentifier: SourceIdentifier)(query: ConditionSQLBuilder[_]) =
    query
      .eq(i.OntologyType, sourceIdentifier.ontologyType)
      .and
      .eq(i.SourceSystem, sourceIdentifier.identifierType.id)
      .and
      .eq(i.SourceId, sourceIdentifier.value)

  private def getRowFromResult(i: SyntaxProvider[Identifier],
                               rs: WrappedResultSet) = {
    (
      rs.string(i.resultName.OntologyType),
      rs.string(i.resultName.SourceSystem),
      rs.string(i.resultName.SourceId))
  }

  private lazy val poolNames = Iterator
    .continually(List('replica, 'primary))
    .flatten

  // Round-robin between the replica and the primary for SELECTs
  private def readOnlySession = ReadOnlyNamedAutoSession(poolNames.next)

  // In MySQL 5.6, the optimiser isn't able to optimise huge index range scans,
  // so we batch the sourceIdentifiers ourselves to prevent these scans.
  //
  // Remember that there is a composite index, (ontologyType, sourceSystem, sourceId)
  // Call the first 2 keys of this the "ID type". A huge range scan happens when
  // the following conditions are satisfied:
  //
  // - there are multiple ID types present in the source identifiers
  // - there are more than one source IDs for any of these ID types
  //
  // This batching is not necessarily optimal but using it is many, many orders
  // of magnitude faster than not using it.
  private def batchSourceIdentifiers(
    sourceIdentifiers: Seq[SourceIdentifier]): Seq[Seq[SourceIdentifier]] = {
    val idTypeGroups =
      sourceIdentifiers
        .groupBy(i => (i.ontologyType, i.identifierType.id))
        .values
    if (idTypeGroups.size > 1 && idTypeGroups.exists(_.size > 1)) {
      idTypeGroups.toSeq
    } else {
      Seq(sourceIdentifiers)
    }
  }

  private def withTimeWarning[R](
    thresholdMillis: Long,
    identifiers: Seq[SourceIdentifier])(f: => R): R = {
    val start = System.currentTimeMillis()
    val result = f
    val end = System.currentTimeMillis()

    val duration = end - start
    if (duration > thresholdMillis) {
      warn(
        s"Query for ${identifiers.size} identifiers (first: ${identifiers.head.value}) took $duration milliseconds",
      )
    }

    result
  }
}
