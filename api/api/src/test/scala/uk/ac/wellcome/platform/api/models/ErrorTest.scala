package uk.ac.wellcome.platform.api.models

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ErrorTest extends AnyFunSpec with Matchers {
  it("creates an HTTP 404 error response") {
    val description = "Work not found for identifier 1234"
    val error: Error =
      Error(variant = ErrorVariant.http404, description = Some(description))

    error.errorType shouldBe "http"
    error.httpStatus.get shouldBe 404
    error.label shouldBe "Not Found"
    error.description.get shouldBe description
  }
}
