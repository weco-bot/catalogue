package uk.ac.wellcome.platform.transformer.miro.transformers

import org.scalatest.Assertion
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.matchers.should.Matchers
import uk.ac.wellcome.models.work.generators.IdentifiersGenerators
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.miro.generators.MiroRecordGenerators
import uk.ac.wellcome.platform.transformer.miro.models.MiroMetadata
import uk.ac.wellcome.platform.transformer.miro.source.MiroRecord

class MiroRecordTransformerTest
    extends AnyFunSpec
    with Matchers
    with IdentifiersGenerators
    with MiroRecordGenerators
    with MiroTransformableWrapper {

  it("passes through the Miro identifier") {
    val miroId = "M0000005_test"
    val work = transformWork(
      createMiroRecordWith(imageNumber = miroId)
    )
    work.identifiers shouldBe List(
      createMiroSourceIdentifierWith(value = miroId)
    )
  }

  it("passes through the INNOPAC ID as the Sierra system number") {
    forAll(Table("", "b", "B", ".b", ".B")) { prefix =>
      forAll(Table("8", "x")) { checkDigit =>
        val innopacID = s"${prefix}1234567$checkDigit"
        val expectedSierraNumber = s"b1234567$checkDigit"

        transformRecordAndCheckSierraSystemNumber(
          innopacID = innopacID,
          expectedSierraNumber = expectedSierraNumber
        )
      }
    }
  }

  describe("non-MPL references are passed through as identifiers") {
    it("no references") {
      transformRecordAndCheckMiroLibraryReferences(
        createMiroRecordWith(
          libraryRefDepartment = Nil,
          libraryRefId = Nil
        ),
        expectedValues = List()
      )
    }

    it("one reference") {
      transformRecordAndCheckMiroLibraryReferences(
        createMiroRecordWith(
          libraryRefDepartment = List(Some("External Reference")),
          libraryRefId = List(Some("Sanskrit ID 1924"))
        ),
        expectedValues = List("External Reference Sanskrit ID 1924")
      )
    }

    it("two references") {
      transformRecordAndCheckMiroLibraryReferences(
        createMiroRecordWith(
          libraryRefDepartment =
            List(Some("External Reference"), Some("ICV No")),
          libraryRefId = List(Some("Sanskrit ID 1924"), Some("1234"))
        ),
        expectedValues = List(
          "External Reference Sanskrit ID 1924",
          "ICV No 1234"
        )
      )
    }

    it("with mismatched ref IDs/department") {
      assertTransformWorkFails(
        createMiroRecordWith(
          libraryRefDepartment = List(Some("External Reference")),
          libraryRefId = List(Some("Sanskrit ID 1924"), Some("1234"))
        )
      )
    }

    it("with ref IDs null but department non-null") {
      assertTransformWorkFails(
        createMiroRecordWith(
          libraryRefDepartment = List(Some("External Reference")),
          libraryRefId = Nil
        )
      )
    }

    it("with ref IDs non-null but department null") {
      assertTransformWorkFails(
        createMiroRecordWith(
          libraryRefDepartment = Nil,
          libraryRefId = List(Some("Sanskrit ID 1924"), Some("1234"))
        )
      )
    }
  }

  it("has no description if no image_image_desc field is present") {
    val work = transformWork(createMiroRecord)
    work.data.description shouldBe None
  }

  it("passes through the value of the description field") {
    val description = "A new novel about northern narwhals in November"
    val work = transformWork(
      createMiroRecordWith(description = Some(description))
    )
    work.data.description shouldBe Some(description)
  }

  describe("Wellcome Images Awards metadata") {
    it("does nothing for non-WIA metadata") {
      val description = "Spotting sea snakes on sandbanks."
      val work = transformWork(
        createMiroRecordWith(
          description = Some(description),
          award = List(Some("Award of Excellence")),
          awardDate = List(None)
        )
      )
      work.data.description shouldBe Some(description)
    }

    it("adds WIA metadata if present") {
      val description = "Purple penguins play with paint."
      val work = transformWork(
        createMiroRecordWith(
          description = Some(description),
          award = List(Some("Biomedical Image Awards")),
          awardDate = List(Some("2001"))
        )
      )
      work.data.description shouldBe Some(
        description + " Biomedical Image Awards 2001.")
    }

    it("only includes WIA metadata") {
      val description = "Giraffes can be grazing, galloping or graceful."
      val work = transformWork(
        createMiroRecordWith(
          description = Some(description),
          award = List(
            Some("Dirt, Wellcome Collection"),
            Some("Biomedical Image Awards")),
          awardDate = List(None, Some("2002"))
        )
      )
      work.data.description shouldBe Some(
        description + " Biomedical Image Awards 2002.")
    }

    it("combines multiple WIA metadata fields if necessary") {
      val description = "Amazed and awe-inspired by an adversarial aardvark."
      val work = transformWork(
        createMiroRecordWith(
          description = Some(description),
          award =
            List(Some("WIA Overall Winner"), Some("Wellcome Image Awards")),
          awardDate = List(Some("2015"), Some("2015"))
        )
      )
      work.data.description shouldBe Some(
        description + " Wellcome Image Awards Overall Winner 2015.")
    }
  }

  it("passes through the value of the creation date on V records") {
    val date = "1820-1848"
    val work = transformWork(
      createMiroRecordWith(
        artworkDate = Some(date),
        imageNumber = "V1234567"
      )
    )
    work.data.createdDate shouldBe Some(Period(date))
  }

  it("does not pass through the value of the creation date on non-V records") {
    val work = transformWork(
      createMiroRecordWith(
        artworkDate = Some("1820-1848"),
        imageNumber = "A1234567"
      )
    )
    work.data.createdDate shouldBe None
  }

  it("passes through the lettering field if available") {
    val lettering = "A lifelong lament for lemurs"
    val work = transformWork(
      createMiroRecordWith(
        suppLettering = Some(lettering)
      )
    )
    work.data.lettering shouldBe Some(lettering)
  }

  it("corrects HTML-encoded entities in the input JSON") {
    val work = transformWork(
      createMiroRecordWith(
        title = Some("A caf&#233; for cats"),
        creator = Some(List(Some("Gyokush&#333;, a c&#228;t &#212;wn&#234;r")))
      )
    )

    work.data.title shouldBe Some("A café for cats")
    work.data.contributors shouldBe List(
      Contributor(
        agent = Agent("Gyokushō, a cät Ôwnêr"),
        roles = Nil
      )
    )
  }

  describe("returns an InvisibleWork") {
    it("if usage restrictions mean we suppress the image") {
      assertTransformReturnsInvisibleWork(
        createMiroRecordWith(
          useRestrictions = Some("Do not use")
        )
      )
    }

    it("if the contributor code is GUS") {
      assertTransformReturnsInvisibleWork(
        createMiroRecordWith(
          sourceCode = Some("GUS"),
          imageNumber = "B0009891"
        )
      )
    }

    it("if the image doesn't have copyright clearance") {
      assertTransformReturnsInvisibleWork(
        createMiroRecordWith(
          copyrightCleared = Some("N")
        )
      )
    }

    it("if the image isn't cleared for the catalogue API") {
      assertTransformReturnsInvisibleWork(
        createMiroRecord,
        miroMetadata = MiroMetadata(isClearedForCatalogueAPI = false)
      )
    }
  }

  it(
    "transforms an image with no credit line and an image-specific contributor code") {
    val work = transformWork(
      createMiroRecordWith(
        creditLine = None,
        sourceCode = Some("FDN"),
        imageNumber = "B0011308"
      )
    )

    val expectedDigitalLocation = DigitalLocation(
      url = "https://iiif.wellcomecollection.org/image/B0011308.jpg/info.json",
      license = Some(License.CCBY),
      credit = Some("Ezra Feilden"),
      locationType = LocationType("iiif-image")
    )
    work.data.items.head.locations shouldBe List(expectedDigitalLocation)
  }

  it("extracts both identifiable and unidentifiable items") {
    val work = transformWork(
      createMiroRecordWith(imageNumber = "B0011308")
    )

    val expectedLocation = DigitalLocation(
      "https://iiif.wellcomecollection.org/image/B0011308.jpg/info.json",
      LocationType("iiif-image"),
      Some(License.CCBY),
      None
    )
    work.data.items shouldBe List(
      Item(id = Unidentifiable, locations = List(expectedLocation))
    )
  }

  it("sets the WorkType as 'Digital Images'") {
    val work = transformWork(createMiroRecord)
    work.data.workType.isDefined shouldBe true
    work.data.workType.get.label shouldBe "Digital Images"
  }

  it("sets the thumbnail with the IIIF Image URL") {
    val miroId = "A0001234"
    val work = transformWork(
      createMiroRecordWith(imageNumber = miroId)
    )

    work.data.thumbnail shouldBe Some(
      DigitalLocation(
        url =
          s"https://iiif.wellcomecollection.org/image/$miroId.jpg/full/300,/0/default.jpg",
        locationType = LocationType("thumbnail-image"),
        license = Some(License.CCBY)
      )
    )
  }

  private def assertTransformReturnsInvisibleWork(
    miroRecord: MiroRecord,
    miroMetadata: MiroMetadata = MiroMetadata(isClearedForCatalogueAPI = true)
  ): Assertion = {
    val triedMaybeWork = transformer.transform(
      miroRecord = miroRecord,
      miroMetadata = miroMetadata,
      version = 1
    )

    triedMaybeWork.isSuccess shouldBe true

    triedMaybeWork.get shouldBe UnidentifiedInvisibleWork(
      sourceIdentifier = createMiroSourceIdentifierWith(
        value = miroRecord.imageNumber
      ),
      version = 1,
      data = WorkData()
    )
  }

  private def transformRecordAndCheckSierraSystemNumber(
    innopacID: String,
    expectedSierraNumber: String
  ): Assertion = {
    val work = transformWork(
      createMiroRecordWith(innopacID = Some(innopacID))
    )
    work.identifiers should contain(
      createSierraSystemSourceIdentifierWith(value = expectedSierraNumber)
    )
  }

  private def transformRecordAndCheckMiroLibraryReferences(
    record: MiroRecord,
    expectedValues: List[String]
  ): Assertion = {
    val miroId = "V0175278"
    val work = transformWork(record.copy(imageNumber = miroId))
    val miroIDList = List(
      createMiroSourceIdentifierWith(value = miroId)
    )
    val libraryRefList = expectedValues.map { value =>
      createSourceIdentifierWith(
        identifierType = IdentifierType("miro-library-reference"),
        value = value
      )
    }
    work.identifiers shouldBe (miroIDList ++ libraryRefList)
  }
}
