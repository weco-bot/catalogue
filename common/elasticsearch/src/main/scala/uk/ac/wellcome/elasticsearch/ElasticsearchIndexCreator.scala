package uk.ac.wellcome.elasticsearch

import com.sksamuel.elastic4s.Index
import com.sksamuel.elastic4s.ElasticDsl.{createIndex, _}
import com.sksamuel.elastic4s.requests.analysis.Analysis
import com.sksamuel.elastic4s.requests.indexes.CreateIndexResponse
import com.sksamuel.elastic4s.requests.indexes.PutMappingResponse
import com.sksamuel.elastic4s.{ElasticClient, Response}
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicMapping
import com.sksamuel.elastic4s.requests.mappings.{MappingDefinition}
import grizzled.slf4j.Logging

import scala.concurrent.{ExecutionContext, Future}

class ElasticsearchIndexCreator(
  elasticClient: ElasticClient,
  index: Index,
  config: IndexConfig)(implicit ec: ExecutionContext)
    extends Logging {
  def create: Future[Unit] = {

    create(index = index, mapping = config.mapping, config.analysis)
  }

  private def create(index: Index,
                     mapping: MappingDefinition,
                     analysis: Analysis): Future[Unit] =
    elasticClient
      .execute {
        createIndex(index.name)
          .mapping { mapping }
          .analysis { analysis }

          // Because we have a relatively small number of records (compared
          // to what Elasticsearch usually expects), we can get weird results
          // if our records are split across multiple shards.
          //
          // e.g. searching for the same query multiple times gets varying results
          //
          // This forces all our records to be indexed into a single shard,
          // which should avoid this problem.
          //
          // If/when we index more records, we should revisit this.
          //
          .shards(1)
          // Elasticsearch has a default maximum number of fields of 1000.
          // Because images have all of the WorkData fields defined twice in the mapping,
          // they end up having more than 1000 fields, so we increase them to 2000
          .settings(Map("mapping.total_fields.limit" -> 2000))
      }
      .flatMap { response: Response[CreateIndexResponse] =>
        if (response.isError) {
          if (response.error.`type` == "resource_already_exists_exception" || response.error.`type` == "index_already_exists_exception") {
            info(s"Index $index already exists")
            update(index, mappingDefinition = mapping)
          } else {
            Future.failed(
              throw new RuntimeException(
                s"Failed creating index $index: ${response.error}"
              )
            )
          }
        } else {
          Future.successful(response)
        }
      }
      .map { _ =>
        info("Index updated successfully")
      }

  private def update(index: Index,
                     mappingDefinition: MappingDefinition): Future[Unit] =
    elasticClient
      .execute {
        putMapping(index.name)
          .dynamic(mappingDefinition.dynamic.getOrElse(DynamicMapping.Strict))
          .as(mappingDefinition.fields)
      }
      .recover {
        case e: Throwable =>
          error(s"Failed updating index $index", e)
          throw e
      }
      .map { response: Response[PutMappingResponse] =>
        if (response.isError) {
          throw new RuntimeException(s"Failed updating index: $response")
        }
        response
      }
      .map { _ =>
        info("Successfully applied new mapping")
      }
}
