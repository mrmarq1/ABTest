package http.routes

import cats.effect.Concurrent
import cats.{Monad, MonadThrow}
import services.TestService.{TestRoutesAlgebra, TestRoutesInterpreter}
import cats._
import cats.effect._
import cats.implicits._
import domain.Test._
import org.http4s.circe._
import org.http4s._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.headers._
import org.http4s.dsl._
import org.http4s.dsl.impl._
import org.http4s.implicits._
import org.http4s.server._
import org.http4s.server.blaze.BlazeServerBuilder


object TestRoutes extends IOApp {
  final def testRoutes[F[_] : Monad : MonadThrow : Concurrent](algebra: TestRoutesAlgebra[F]): HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._
    implicit val newTestEntityDecoder: EntityDecoder[F, NewTest] = CirceEntityDecoder.circeEntityDecoder

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
