package uk.ac.wellcome.platform.merger.rules
import uk.ac.wellcome.models.work.internal.{
  TransformedBaseWork,
  UnidentifiedWork
}
import uk.ac.wellcome.platform.merger.models.FieldMergeResult
import uk.ac.wellcome.platform.merger.rules.WorkPredicates.WorkPredicate

/*
 * A trait to extend in order to merge fields of the type member `Field`
 *
 * The implementor must provide the method `merge`, which takes a target
 * work and a list of source works, and returns the new value of the field
 * as well as a list of source works that this rule would like to be redirected.
 *
 * Many merge rules will want to apply different logic given conditions on
 * both the target and the source works.
 * The PartialRule trait is provided to achieve this: in addition to
 * a `rule` that has the same signature as the `merge` method, the implementor
 * must provide predicates for valid targets and sources.
 *
 * Because PartialRule is a PartialFunction, the `rule` will never be called
 * for targets/sources that don't satisfy these predicates, and we gain things
 * like `orElse` and `andThen` for free.
 */
trait FieldMergeRule {
  protected final type Params = (UnidentifiedWork, Seq[TransformedBaseWork])
  protected type FieldData

  def merge(target: UnidentifiedWork,
            sources: Seq[TransformedBaseWork]): FieldMergeResult[FieldData]

  protected val identityOnTarget: PartialFunction[Params, UnidentifiedWork] = {
    case (target, _) => target
  }

  protected trait PartialRule extends PartialFunction[Params, FieldData] {
    val isDefinedForTarget: WorkPredicate
    val isDefinedForSource: WorkPredicate

    def rule(target: UnidentifiedWork,
             sources: Seq[TransformedBaseWork]): FieldData

    override def apply(params: Params): FieldData =
      params match {
        case (target, sources) =>
          rule(target, sources.filter(isDefinedForSource))
      }

    override def isDefinedAt(params: Params): Boolean = params match {
      case (target, sources) =>
        isDefinedForTarget(target) && sources.exists(isDefinedForSource)
    }
  }
}
