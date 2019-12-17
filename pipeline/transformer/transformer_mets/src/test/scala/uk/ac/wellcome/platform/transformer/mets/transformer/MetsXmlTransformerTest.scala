package uk.ac.wellcome.platform.transformer.mets.transformer

import org.scalatest.{FunSpec, Matchers}
import org.apache.commons.io.IOUtils

import uk.ac.wellcome.mets_adapter.models.MetsLocation
import uk.ac.wellcome.platform.transformer.mets.fixtures.MetsGenerators
import uk.ac.wellcome.storage.store.memory.MemoryStore
import uk.ac.wellcome.storage.ObjectLocation

class MetsXmlTransformerTest extends FunSpec with Matchers with MetsGenerators {

  it("should transform METS XML") {
    val xml = loadXmlFile("/b30246039.xml")
    transform(Some(xml)) shouldBe Right(
      MetsData(
        "b30246039",
        Some("CC-BY-NC"),
        Some("b30246039_0001.jp2")
      )
    )
  }

  it("should error when the root XML doesn't exist in the store") {
    transform(None) shouldBe a[Left[_, _]]
  }

  it("should transform METS XML with manifestations") {
    val xml = loadXmlFile("/b22012692.xml")
    val manifestations = Map(
      "b22012692_0003.xml" -> Some(loadXmlFile("/b22012692_0003.xml")),
      "b22012692_0001.xml" -> Some(loadXmlFile("/b22012692_0001.xml")),
    )
    transform(Some(xml), manifestations) shouldBe Right(
      MetsData(
        "b22012692",
        Some("PDM"),
        Some("b22012692_0001_0001.jp2")
      )
    )
  }

  it("should error if first manifestation doesn't exist in store") {
    val xml = loadXmlFile("/b22012692.xml")
    val manifestations = Map(
      "b22012692_0003.xml" -> Some(loadXmlFile("/b22012692_0003.xml")),
      "b22012692_0001.xml" -> None,
    )
    transform(Some(xml), manifestations) shouldBe a[Left[_, _]]
  }

  def transform(root: Option[String],
                manifestations: Map[String, Option[String]] = Map.empty) = {

    val metsLocation = MetsLocation(
      "bucket",
      "path",
      1,
      if (root.nonEmpty) "root.xml" else "nonexistent.xml",
      manifestations.toList.map { case (file, _) => file }
    )

    val store = new MemoryStore(
      (manifestations ++ root
        .map(content => "root.xml" -> Some(content))).collect {
        case (file, Some(content)) =>
          ObjectLocation("bucket", s"path/$file") -> content
      }.toMap
    )

    new MetsXmlTransformer(store).transform(metsLocation)
  }

  def loadXmlFile(path: String) =
    IOUtils.toString(getClass.getResourceAsStream(path), "UTF-8")
}