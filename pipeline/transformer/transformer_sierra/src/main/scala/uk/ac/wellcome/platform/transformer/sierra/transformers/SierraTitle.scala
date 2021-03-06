package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.platform.transformer.sierra.exceptions.ShouldNotTransformException
import uk.ac.wellcome.platform.transformer.sierra.source.SierraBibData
import uk.ac.wellcome.sierra_adapter.model.SierraBibNumber

object SierraTitle extends SierraTransformer {

  type Output = Option[String]

  // Populate wwork:title.  The rules are as follows:
  //
  //    For all bibliographic records use Sierra "title".
  //
  // Note: Sierra populates this field from MARC field 245 subfields $a and $b.
  // http://www.loc.gov/marc/bibliographic/bd245.html
  def apply(bibId: SierraBibNumber, bibData: SierraBibData) =
    Some(
      bibData.title.getOrElse(
        throw new ShouldNotTransformException(s"Sierra record has no title!")))
}
