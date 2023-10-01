package services

import cats.Monad

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
    def addTest(newTest: NewTest): F[Option[mutable.ArrayBuffer[Test]]]
    // PUT
    def updateTestName(testId: TestId): F[Option[mutable.ArrayBuffer[Test]]]
    def updateTestStatus(testId: TestId): F[Option[mutable.ArrayBuffer[Test]]]
    // GET
    def getTestById(testId: TestId)(idMappedDb: IdMappedDb): F[Option[Test]]
    def getTestsByBrand(brand: Brand)(brandMappedDb: BrandMappedDb): F[Option[List[Test]]]
  }

  // Interpreter
  case class TestRoutesInterpreter[F[_]: Monad]() extends TestRoutesAlgebra[F] {
    private def createTest(newTest: NewTest): F[Test] = {
      val testId: TestId = TestId(UUID.randomUUID())
      val adVariants: LazyList[AdVariant] = newTest.adTextVariants.map(adText => AdVariant(UUID.randomUUID(), adText))
      val testStatus: TestStatus = Pending
      val test = Test(testId, newTest.brand, newTest.testName, adVariants, newTest.testDuration, testStatus)
      Monad[F].pure(test)
    }

    def addTest(newTest: NewTest): F[Option[mutable.ArrayBuffer[Test]]] = {
      val test = createTest(newTest)
      tests += test
    }

    def updateTestName(testId: TestId): F[Option[mutable.ArrayBuffer[Test]]] = {
      ???
    }

    def updateTestStatus(testId: TestId): F[Option[mutable.ArrayBuffer[Test]]] = {
      ???
    }

    def getTestById(testId: TestId)(idMappedDb: IdMappedDb): F[Option[Test]] = {
      ???
    }

    def getTestsByBrand(brand: Brand)(brandMappedDb: BrandMappedDb): F[Option[List[Test]]] = {
      ???
    }
  }
}
