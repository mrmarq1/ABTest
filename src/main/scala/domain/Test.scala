package domain

import io.estatico.newtype.macros.newtype

import java.util.UUID

import Brand._
import Advert._

object Test {
  @newtype
  case class Test(testId: TestId, brand: Brand, testName: TestName, adVariants: LazyList[AdVariant],
                  testDuration: Double, testStatus: TestStatus)

  @newtype
  case class TestId(testId: UUID)

  @newtype
  case class TestName(testName: String)

  sealed trait TestStatus
  case object Pending extends TestStatus
  case object Live extends TestStatus
  case object Paused extends TestStatus
  case object Ended extends TestStatus

  @newtype
  case class NewTest(brand: Brand, testName: TestName, adTextVariants: LazyList[AdText], testDuration: Double)
}



