package uk.ac.wellcome.platform.transformer.sierra.exceptions

import uk.ac.wellcome.sierra_adapter.model.SierraBibNumber

// Thrown if the data has a cataloguing error -- that is, the transformer
// cannot handle it without a change in the source data.
//
// These errors are reported to a separate queue.
//
class CataloguingException(bibId: SierraBibNumber, message: String)
    extends SierraTransformerException(
      new RuntimeException(
        s"Problem in the Sierra data for ${bibId.withCheckDigit}: $message"
      )
    )

case object CataloguingException {
  def apply(bibId: SierraBibNumber, message: String): CataloguingException =
    new CataloguingException(bibId = bibId, message = message)
}
