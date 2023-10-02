package http.routes

import cats.effect.Concurrent
import cats.{Monad, MonadThrow}
import services.TestService.{TestRoutesAlgebra, TestRoutesInterpreter}
import cats.effect._
import cats.implicits._
import domain.Brand._
import domain.ABTest._
import org.http4s.circe._
import org.http4s._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import io.circe.{Encoder, Json}
import org.http4s.dsl._
import org.http4s.implicits._
import org.http4s.server._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.dsl.impl.QueryParamDecoderMatcher

object TestRoutes extends IOApp {

  object BrandQueryParamMatcher extends QueryParamDecoderMatcher[String]("brand")

  def testRoutes[F[_] : Monad : MonadThrow : Concurrent](algebra: TestRoutesAlgebra[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    implicit val testStatusEncoder: Encoder[TestStatus] = new Encoder[TestStatus] {
      override def apply(status: TestStatus): Json = status match {
        case Pending => Json.fromString("Pending")
        case Live => Json.fromString("Live")
        case Paused => Json.fromString("Paused")
        case Ended => Json.fromString("Ended")
      }
    }
    implicit val testUpdateEncoder: Encoder[TestUpdate] = new Encoder[TestUpdate] {
      override def apply(update: TestUpdate): Json = update match {
        case NoUpdate => Json.fromString("NoUpdate")
        case TestNameUpdated => Json.fromString("TestNameUpdated")
        case BrandNameUpdated => Json.fromString("BrandNameUpdated")
      }
    }

    HttpRoutes.of[F] {
      case req@POST -> Root / "submit" =>
        req.as[NewTest].flatMap { newTest =>
          val response = algebra.addTest(newTest)
          val isEmptyF: F[Boolean] = response.map(testArray => testArray.isEmpty)

          isEmptyF.flatMap { isEmpty =>
            if (isEmpty) {
              InternalServerError("Database connection failed. Please try again.")
            } else {
              Created(s"Test successfully submitted. Execution pending...")
            }
          }
        }

      case GET -> Root / "searchBrand" :? BrandQueryParamMatcher(brand) =>
        algebra.getTestsByBrand(BrandName(brand)).flatMap {
          case Some(brandTests) => Ok(brandTests.asJson)
          case _ => NotFound(s"'$brand' currently has no tests in the database")
        }
    }
  }

  override def run(args: List[String]): IO[ExitCode] = {
    val testRoutesIO: HttpRoutes[IO] = testRoutes(new TestRoutesInterpreter[IO])

    val apis = Router(
      "/v1/tests/" -> testRoutesIO
    ).orNotFound

    BlazeServerBuilder[IO](runtime.compute)
      .bindHttp(8080, "localhost")
      .withHttpApp(apis)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }
}
