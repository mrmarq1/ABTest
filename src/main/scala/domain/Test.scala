package domain

import java.util.UUID
import Brand._
import Advert._

import java.time.LocalDateTime

object Test {
  case class Test(testId: TestId, brand: Brand, testName: TestName, adVariants: Vector[AdVariant], testSpend: BigDecimal,
                  testDuration: Double, testSubmissionDate: LocalDateTime, testStartDate: LocalDateTime,
                  testStatus: TestStatus, testUpdate: TestUpdate)

  case class TestId(testId: UUID)

  case class TestName(testName: String)

  trait TestStatus
  case object Pending extends TestStatus
  case object Live extends TestStatus
  case object Paused extends TestStatus
  case object Ended extends TestStatus


  trait TestUpdate
  case object NoUpdate extends TestUpdate
  case object TestNameUpdated extends TestUpdate
  case object BrandNameUpdated extends TestUpdate

  case class NewTest(brand: Brand, testName: TestName, adTextVariants: Vector[variantText],
                     testSpend: BigDecimal, testDuration: Double)
}



