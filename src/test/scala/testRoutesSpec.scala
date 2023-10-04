import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import http.routes.TestRoutes
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.http4s._
import org.http4s.implicits._
import org.http4s.headers.`Content-Type`
import io.circe.Json
import services.TestService.{TestRoutesAlgebra, TestRoutesInterpreter}
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server._

import scala.concurrent.{ExecutionContext, Future}

class TestRoutesSpec extends AsyncWordSpec with Matchers {

  val testRoutesInterpreter: TestRoutesAlgebra[IO] = new TestRoutesInterpreter[IO]

  val testServerResource: Resource[IO, Server] =
    BlazeServerBuilder[IO](ExecutionContext.global)
      .bindHttp(port = 8080)
      .withHttpApp(TestRoutes.testRoutes[IO](testRoutesInterpreter).orNotFound)
      .resource

  val request = (requestBody: Json, method: Method, uri: Uri) => Request[IO](method, uri)
    .withEntity(requestBody.noSpaces)
    .withHeaders(`Content-Type`(MediaType.application.json))

  val futureResponse = (requestBody: Json, method: Method, uri: Uri) => TestRoutes
    .testRoutes[IO](testRoutesInterpreter)
    .orNotFound(request(requestBody, method, uri))
    .unsafeToFuture()

  "TestRoutes" when {
    "given a valid POST/addTest request" should {
      "trigger a 200 'Ok' with execution pending text" in {
        val validPostRequest: Json = Json.obj(
          "brandName" -> Json.obj("brandName" -> Json.fromString("Company XYZ Ltd")),
          "testName" -> Json.obj("testName" -> Json.fromString("Another test")),
          "adTextVariants" -> Json.arr(Json.fromString("Hello 1"), Json.fromString("Hello 2"), Json.fromString("Hello 3")),
          "testSpend" -> Json.fromBigDecimal(BigDecimal(1500000)),
          "testDuration" -> Json.fromDoubleOrNull(160.00)
        )

        futureResponse(validPostRequest, Method.POST, uri"/submit").map { response =>
          val responseBodyText = response.bodyText.compile.string.unsafeRunSync()
          responseBodyText shouldBe "Test successfully submitted. Execution pending..."
          response.status shouldBe Status.Ok
        }
      }
    }

    "given an invalid POST/addTest request" should {
      "trigger a 400 'Bad Request' with error messages for each invalid field" in {
        val invalidPostRequest: Json = Json.obj(
          "brandName" -> Json.obj("brandName" -> Json.fromString("Company XYZ Ltd!")),
          "testName" -> Json.obj("testName" -> Json.fromString("Another test*")),
          "adTextVariants" -> Json.arr(Json.fromString("$Hello 1"), Json.fromString("Hello 2"), Json.fromString("Hello 3")),
          "testSpend" -> Json.fromBigDecimal(BigDecimal(1500000)),
          "testDuration" -> Json.fromDoubleOrNull(160.00)
        )

        futureResponse(invalidPostRequest, Method.POST, uri"/submit").map { response =>
          val responseBodyText = response.bodyText.compile.string.unsafeRunSync()
          responseBodyText shouldBe "Invalid format for: Company XYZ Ltd!. Please use only letters, numbers or the special characters ,.&'. " +
            "Character limit is 30.\nInvalid format for: Another test*. Please use only letters, numbers or the special characters ,.&'. " +
            "Character limit is 30.\nInvalid format for: $Hello 1. Please use only letters, numbers or the special characters ,.!?'. " +
            "Character limit is 120."
          response.status shouldBe Status.BadRequest
        }
      }
    }

    "given an valid GET/getTestsByBrand request" should {
      "trigger a 200 'Ok' with a json response for all Tests found" in {
        futureResponse(Json.Null, Method.GET, uri"/searchBrand?brand=Company%20B").map { response =>
          val responseBodyText = response.bodyText.compile.string.unsafeRunSync()
          responseBodyText shouldBe "[{\"testId\":{\"testId\":\"75402ece-cf57-43e0-b534-296c44188e47\"},\"brandName\":{\"brandName\":\"Company B\"}," +
                                    "\"testName\":{\"testName\":\"test3\"},\"adVariants\":[{\"variantId\":\"981cad7f-a8f3-426a-8a1e-6ff322c0b6f2\"," +
                                    "\"variantText\":\"Text1\",\"variantSpend\":350000,\"variantDropped\":false},{\"variantId\":\"ffc656dc-ee19-4d3e-883c-ff0625f94f09\"," +
                                    "\"variantText\":\"Text2\",\"variantSpend\":0,\"variantDropped\":true}],\"testSpend\":500000,\"testDuration\":945.5," +
                                    "\"testSubmissionDate\":\"2023-01-07T13:57:00\",\"testStartDate\":\"2023-02-15T23:27:00\",\"testStatus\":\"Ended\"," +
                                    "\"testUpdate\":\"NoUpdate\"}]"
          response.status shouldBe Status.Ok
        }
      }
    }

    "given an invalid GET/getTestsByBrand request (brand not found)" should {
      "trigger a 404 'Not Found' with a message stating no tests found for the given brand" in {
        futureResponse(Json.Null, Method.GET, uri"/searchBrand?brand=Company%20D").map { response =>
          val responseBodyText = response.bodyText.compile.string.unsafeRunSync()
          responseBodyText shouldBe "'Company D' currently has no tests in the database"
          response.status shouldBe Status.NotFound
        }
      }
    }

    "given an invalid GET/getTestsByBrand request (validation failure)" should {
      "trigger a 400 'Bad Request' with a message stating accepted characters" in {
        futureResponse(Json.Null, Method.GET, uri"/searchBrand?brand=?Company%20D").map { response =>
          val responseBodyText = response.bodyText.compile.string.unsafeRunSync()
          responseBodyText shouldBe "Invalid brand name format. Only letters and numbers allowed."
          response.status shouldBe Status.BadRequest
        }
      }
    }
  }
}