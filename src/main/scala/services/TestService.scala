package services

import cats.effect.{IO, MonadCancel, Resource, Sync}
import cats.{Applicative, ApplicativeThrow, Monad, MonadError}
import cats.implicits._
import cats.syntax.all._

import scala.collection.mutable
import java.util.UUID
import domain.Advert.AdVariant
import domain.Test._
import domain.Brand._
import persistence.PracticeDb._

object TestService {
  // Algebra
  trait TestRoutesAlgebra[F[_]] {
    // POST
    def addTest(newTest: NewTest): F[mutable.ArrayBuffer[Test]]
    // PUT
    def updateTestName(testId: TestId): F[mutable.ArrayBuffer[Test]]
    def updateTestStatus(testId: TestId): F[mutable.ArrayBuffer[Test]]
    // GET
    def getTestById(testId: TestId)(idMappedDb: IdMappedDb): F[Test]
    def getTestsByBrand(brand: Brand)(brandMappedDb: BrandMappedDb): F[List[Test]]
  }

  def createTest(newTest: NewTest): Test = {
    val testId: TestId = TestId(UUID.randomUUID())
    val adVariants: LazyList[AdVariant] = newTest.adTextVariants.map(adText => AdVariant(UUID.randomUUID(), adText))
    val testStatus: TestStatus = Pending
    Test(testId, newTest.brand, newTest.testName, adVariants, newTest.testDuration, testStatus)
  }

  // Temporary definitions
  case class DatabaseConnection(db: mutable.ArrayBuffer[Test])
  val connection = DatabaseConnection(testsDb)

  // Interpreter
  case class TestRoutesInterpreter[F[_], A](implicit mc: MonadCancel[F, Throwable]) extends TestRoutesAlgebra[F] {
    // Method implemented ahead of transitioning practice database and requiring a network connection
    def dbConnection(): Resource[F, DatabaseConnection] =
      Resource.make(MonadError[F, Throwable].catchNonFatal(connection)) {
        connection => Applicative[F].pure(println(s"Releasing database connection: '$connection''"))
      }

    def addTest(newTest: NewTest): F[mutable.ArrayBuffer[Test]] = {
      val test = createTest(newTest)
      val connectionDb = dbConnection()
      connectionDb.use { connection =>
        val testsDb = connection.db
        testsDb += test
        Monad[F].pure(testsDb)
      }.handleErrorWith { error =>
        println(error)
        Monad[F].pure(mutable.ArrayBuffer.empty[Test])
      }
    }

    def updateTestName(testId: TestId): F[mutable.ArrayBuffer[Test]] = {
      ???
    }

    def updateTestStatus(testId: TestId): F[mutable.ArrayBuffer[Test]] = {
      ???
    }

    def getTestById(testId: TestId)(idMappedDb: IdMappedDb): F[Test] = {
      ???
    }

    def getTestsByBrand(brand: Brand)(brandMappedDb: BrandMappedDb): F[List[Test]] = {
      ???
    }
  }
}
