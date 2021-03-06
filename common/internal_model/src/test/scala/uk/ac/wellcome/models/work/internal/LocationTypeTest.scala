package uk.ac.wellcome.models.work.internal

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class LocationTypeTest extends AnyFunSpec with Matchers {
  it("looks up a location type") {
    LocationType("arch") shouldBe LocationType(
      id = "arch",
      label = "Archives Collection"
    )
  }

  it("throws an error if looking up a non-existent location type") {
    val caught = intercept[IllegalArgumentException] {
      LocationType(id = "DoesNotExist")
    }
    caught.getMessage shouldBe "Unrecognised location type: [DoesNotExist]"
  }
}
