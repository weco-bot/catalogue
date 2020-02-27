package uk.ac.wellcome.platform.transformer.calm

import uk.ac.wellcome.models.work.internal.{
  SourceIdentifier,
  TransformedBaseWork,
  UnidentifiedWork,
  WorkData
}
import uk.ac.wellcome.platform.transformer.calm.models.CalmRecord

trait CalmTransformerError extends Throwable
case object SourceIdentifierMissingError extends CalmTransformerError
case object MultipleSourceIdentifiersFoundError extends CalmTransformerError

object CalmTransformer extends Transformer[CalmRecord] {
  def transform(
    record: CalmRecord): Either[CalmTransformerError, TransformedBaseWork] =
    Right(
      UnidentifiedWork(
        sourceIdentifier = SourceIdentifier(
          value = record.id,
          identifierType = CalmIdentifierTypes.recordId,
          ontologyType = "IdentifierType"),
        version = 1,
        data = WorkData()
      )
    )
}