package uk.ac.wellcome.mets_adapter.models

case class IngestUpdate(space: Space, bag: StubBag)

case class Space(id: String)
sealed trait BaseBag {
  val info: BagInfo
}
case class StubBag(info: BagInfo) extends BaseBag
case class Bag(info: BagInfo, manifest: BagManifest, location: BagLocation)
    extends BaseBag
case class BagInfo(externalIdentifier: String)

case class BagManifest(files: List[BagFile])

case class BagLocation(bucket: String, path: String)

case class BagFile(name: String, path: String)