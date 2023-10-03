package http.routes

import cats.data.Validated
import cats.effect.Concurrent
import cats.{Applicative, ApplicativeError, ApplicativeThrow, Monad, MonadThrow}
import services.TestService.{TestRoutesAlgebra, TestRoutesInterpreter}
import cats.effect._
import cats.implicits._
import domain.ABTest._
import domain.Brand.BrandName
import eu.timepit.refined.api.Refined
import eu.timepit.refined.{W, api, refineV}
import eu.timepit.refined.string.MatchesRegex
import org.http4s.circe._
import org.http4s._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.deriveDecoder
import org.http4s.dsl._
import org.http4s.implicits._
import org.http4s.server._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.dsl.impl.{OptionalValidatingQueryParamDecoderMatcher, QueryParamDecoderMatcher}

import java.nio.charset.MalformedInputException
import java.util.UUID
import scala.util.Try

object TestRoutes extends IOApp {
  implicit val idQueryParamDecoder: QueryParamDecoder[UUID] = QueryParamDecoder[String].emap { idString =>
    Try(UUID.fromString(idString)).toEither.leftMap { e => ParseFailure(e.getMessage, e.getMessage) }
  }

  private object TestIdQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[UUID]("testId")
  private object BrandQueryParamMatcher extends QueryParamDecoderMatcher[String]("brand")

  private final def testRoutes[F[_] : Monad : MonadThrow : Concurrent](algebra: TestRoutesAlgebra[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    // Provide implicits to enable Circe encoding of sum type ADTs
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

    // Example POST runtime validation
    def isValid(text: String): Validated[String, String] =
      if (text.matches("^[a-zA-Z0-9\\s,.!?']+")) {
        Validated.valid(text)
      }
      else Validated.invalid(s"Invalid format for: $text. Please use only letters, numbers or the special characters ,.!?';")

    implicit val newTestEntityDecoder: EntityDecoder[F, NewTest] = jsonOf[F, NewTest].flatMapR { newTest =>
      (isValid(newTest.brandName.brandName),
        isValid(newTest.testName.testName),
        newTest.adTextVariants.map(text => isValid(text)).sequence
      ).mapN { (_, _, _) => newTest // to simply return newTest if validation passed
      }.fold (
        errors => DecodeResult.failure(ApplicativeThrow[F].raiseError(MalformedMessageBodyFailure(errors.split(";").mkString("\n")))),
        result => DecodeResult.success(Applicative[F].pure(result))
      )
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
              Ok(s"Test successfully submitted. Execution pending...")
            }
          }
        }.recoverWith {
          case MalformedMessageBodyFailure(errorMessage, _) =>
            BadRequest(errorMessage)
        }

      case GET -> Root / "searchBrand" :? BrandQueryParamMatcher(brand) =>
        refineV[MatchesRegex[W.`"""^[a-zA-Z0-9\\s%]+"""`.T]](brand) match {
          case Right(validBrand) =>
            algebra.getTestsByBrand(BrandName(validBrand.value)).flatMap {
              case Some(brandTests) => Ok(brandTests.asJson)
              case _ => NotFound(s"'$brand' currently has no tests in the database")
            }
          case Left(_) =>
            BadRequest("Invalid brand name format. Only letters and numbers allowed.")
        }


      case GET -> Root / "searchId" :? TestIdQueryParamMatcher(testId) =>
        testId match {
          case Some(validatedUUID) =>
            validatedUUID.fold(
              _ => BadRequest("Invalid UUID format for test ID"),
              uuid => {
                algebra.getTestById(TestId(uuid)).flatMap {
                  case Some(test) => Ok(test.asJson)
                  case _ => NotFound(s"No tests found for the id $uuid")
                }
              }
            )
          case _ => BadRequest("No test ID entered")
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
