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
        val requestBodyOne: Json = Json.obj(
          "brandName" -> Json.obj("brandName" -> Json.fromString("Company XYZ Ltd")),
          "testName" -> Json.obj("testName" -> Json.fromString("Another test")),
          "adTextVariants" -> Json.arr(Json.fromString("Hello 1"), Json.fromString("Hello 2"), Json.fromString("Hello 3")),
          "testSpend" -> Json.fromBigDecimal(BigDecimal(1500000)),
          "testDuration" -> Json.fromDoubleOrNull(160.00)
        )

        futureResponse(requestBodyOne, Method.POST, uri"/submit").map { response =>
          val responseBodyText = response.bodyText.compile.string.unsafeRunSync()
          responseBodyText shouldBe "Test successfully submitted. Execution pending..."
          response.status shouldBe Status.Ok
        }
      }
    }
    "given an invalid POST/addTest request" should {
      "trigger a 400 'BadRequest' with error messages for each invalid field" in {
        val requestBodyTwo: Json = Json.obj(
          "brandName" -> Json.obj("brandName" -> Json.fromString("Company XYZ Ltd!")),
          "testName" -> Json.obj("testName" -> Json.fromString("Another test*")),
          "adTextVariants" -> Json.arr(Json.fromString("$Hello 1"), Json.fromString("Hello 2"), Json.fromString("Hello 3")),
          "testSpend" -> Json.fromBigDecimal(BigDecimal(1500000)),
          "testDuration" -> Json.fromDoubleOrNull(160.00)
        )

        futureResponse(requestBodyTwo, Method.POST, uri"/submit").map { response =>
          val responseBodyText = response.bodyText.compile.string.unsafeRunSync()
          responseBodyText shouldBe "Invalid format for: Company XYZ Ltd!. Please use only letters, numbers or the special characters ,.&'. " +
            "Character limit is 30.\nInvalid format for: Another test*. Please use only letters, numbers or the special characters ,.&'. " +
            "Character limit is 30.\nInvalid format for: $Hello 1. Please use only letters, numbers or the special characters ,.!?'. " +
            "Character limit is 120."
          response.status shouldBe Status.BadRequest
        }
      }
    }
  }
}