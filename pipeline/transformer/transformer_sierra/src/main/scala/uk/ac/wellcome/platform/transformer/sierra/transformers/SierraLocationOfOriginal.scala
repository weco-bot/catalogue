package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.transformable.sierra.SierraBibNumber
import uk.ac.wellcome.platform.transformer.sierra.source.{
  SierraBibData,
  SierraQueryOps
}

object SierraLocationOfOriginal extends SierraTransformer with SierraQueryOps {

  type Output = Option[String]

  def apply(bibId: SierraBibNumber, bibData: SierraBibData) =
    bibData
      .subfieldsWithTag("535" -> "a")
      .firstContent
}