package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.work.internal.WorkType
import uk.ac.wellcome.platform.transformer.sierra.data.SierraMaterialTypes
import uk.ac.wellcome.platform.transformer.sierra.source.SierraBibData
import uk.ac.wellcome.sierra_adapter.model.SierraBibNumber

object SierraWorkType extends SierraTransformer {

  type Output = Option[WorkType]

  /* Populate wwork:workType. Rules:
   *
   * 1. For all bibliographic records use "materialType"
   * 2. Platform "id" is populated from "code"
   * 3. Platform "label" is populated from "value"
   *
   * Example:
   *  "workType": {
   *     "id": "e-book",
   *     "type": "WorkType",
   *     "label": "E-books"
   *     },
   *
   * Note: will map to a controlled vocabulary terms in future
   */
  def apply(bibId: SierraBibNumber, bibData: SierraBibData) =
    bibData.materialType.map { t =>
      SierraMaterialTypes.fromCode(t.code)
    }
}
