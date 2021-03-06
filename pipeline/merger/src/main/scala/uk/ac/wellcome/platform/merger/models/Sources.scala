package uk.ac.wellcome.platform.merger.models

import uk.ac.wellcome.models.work.internal.{
  TransformedBaseWork,
  UnidentifiedWork
}

object Sources {
  def findFirstLinkedDigitisedSierraWorkFor(
    target: UnidentifiedWork,
    sources: Seq[TransformedBaseWork]): Option[TransformedBaseWork] = {

    val digitisedLinkedIds = target.data.mergeCandidates
      .filter(_.reason.contains("Physical/digitised Sierra work"))
      .map(_.identifier)

    sources.find(source => digitisedLinkedIds.contains(source.sourceIdentifier))
  }
}
