package domain

import java.util.UUID

import Brand._
import Advert._

object Test {
  case class Test(testId: TestId, brand: Brand, testName: TestName, adVariants: Vector[AdVariant],
                  testSpend: BigDecimal, testDuration: Double, testStatus: TestStatus)

  case class TestId(testId: UUID)

  case class TestName(testName: String)

  trait TestStatus
  case object Pending extends TestStatus
  case object Live extends TestStatus
  case object Paused extends TestStatus
  case object Ended extends TestStatus

  case class NewTest(brand: Brand, testName: TestName, adTextVariants: Vector[variantText], testSpend: BigDecimal, testDuration: Double)
}



