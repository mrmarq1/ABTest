package services

import cats.effect.{MonadCancel, Resource}
import cats.{Applicative, Monad, MonadError}
import cats.implicits._

import scala.collection.mutable
import java.util.UUID
import domain.Advert.AdVariant
import domain.ABTest._
import domain.Brand._
import persistence.PracticeDb._

import java.time.LocalDateTime

object TestService {
  // Algebra
  trait TestRoutesAlgebra[F[_]] {
    // POST
    def addTest(newTest: NewTest): F[mutable.ArrayBuffer[Test]]
    // PUT
    def updateTestName(testId: TestId, newTestName: TestName): F[mutable.ArrayBuffer[Test]]
    def updateTestStatus(testId: TestId): F[mutable.ArrayBuffer[Test]]
    // GET
    def getTestById(testId: TestId): F[Option[Test]]
    def getTestsByBrand(brand: BrandName): F[Option[List[Test]]]
  }

  def createTest(newTest: NewTest): Test = {
    val testId: TestId = TestId(UUID.randomUUID())
    val variantSpend: BigDecimal = newTest.testSpend / newTest.adTextVariants.length
    val adVariants: Vector[AdVariant] = newTest.adTextVariants.map(adText => AdVariant(UUID.randomUUID(), adText, variantSpend, false))
    val testSubmissionDate: LocalDateTime = LocalDateTime.now()
    val testStartDate: LocalDateTime = emptyDateTime
    val testStatus: TestStatus = Pending
    val testUpdate: TestUpdate = NoUpdate
    Test(testId, newTest.brandName, newTest.testName, adVariants, newTest.testSpend, newTest.testDuration,
      testSubmissionDate, testStartDate, testStatus, testUpdate)
  }

  // Temporary definitions
  case class DatabaseConnection(db: mutable.ArrayBuffer[Test])
  val connection = DatabaseConnection(testsDb)

  // Interpreter
  case class TestRoutesInterpreter[F[_]]()(implicit mc: MonadCancel[F, Throwable]) extends TestRoutesAlgebra[F] {
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

    def updateTestName(testId: TestId, newTestName: TestName): F[mutable.ArrayBuffer[Test]] = {
      ???
    }

    def updateTestStatus(testId: TestId): F[mutable.ArrayBuffer[Test]] = {
      ???
    }

    def getTestById(testId: TestId): F[Option[Test]] = {
      val testById = idMappedDb().get(testId)
      Monad[F].pure(testById.flatMap(_.headOption))
    }

    def getTestsByBrand(brandName: BrandName): F[Option[List[Test]]] = {
      println(brandName)
      val testsByBrand = brandMappedDb().get(brandName).map(_.toList)
      Monad[F].pure(testsByBrand)
    }
  }
}
