package uk.ac.wellcome.platform.merger.rules
import uk.ac.wellcome.models.work.internal.{BaseWork, TransformedBaseWork, UnidentifiedWork}
import uk.ac.wellcome.platform.merger.model.MergedWork

trait MergerRule { this: Partitioner with WorkPairMerger =>

  def mergeAndRedirectWorks(works: Seq[BaseWork]): Seq[BaseWork] =
    partitionWorks(works)
      .map {
        case Partition(firstWork, secondWork, otherWorks) =>
          val maybeResult = mergeAndRedirectWorkPair(
            firstWork = firstWork,
            secondWork = secondWork
          )
          maybeResult match {
            case Some(result) =>
              updateVersion(result) ++ otherWorks
            case _ => works
          }
      }
      .getOrElse(works)

  private def updateVersion(mergedWork: MergedWork): Seq[BaseWork] =
    mergedWork match {
      case MergedWork(work, redirectedWork) =>
        List(
          work.withData(_.copy(merged = true)),
          redirectedWork
        )
    }
}

case class Partition(firstWork: UnidentifiedWork,
                     secondWork: TransformedBaseWork,
                     otherWorks: Seq[BaseWork])

trait Partitioner {
  protected def partitionWorks(works: Seq[BaseWork]): Option[Partition]
}

trait WorkPairMerger {
  protected def mergeAndRedirectWorkPair(
    firstWork: UnidentifiedWork,
    secondWork: TransformedBaseWork): Option[MergedWork]
}
