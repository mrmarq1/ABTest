package persistence

import domain.Test._
import domain.Advert._
import domain.Brand._

import java.util.UUID
import scala.collection.mutable

object PracticeDb {
  val testId1: TestId = TestId(UUID.fromString("0c50d92b-3a3c-45fa-bd1b-08f4b1e9dcac"))
  val testId2: TestId = TestId(UUID.fromString("811c79f4-8e94-4721-9932-4d7a889c0261"))
  val testId3: TestId = TestId(UUID.fromString("75402ece-cf57-43e0-b534-296c44188e47"))
  val testId4: TestId = TestId(UUID.fromString("af591b46-c48a-4f8d-9f1d-863ec558ad68"))

  val brand1: Brand = Brand("Company A")
  val brand2: Brand = Brand("Company B")
  val brand3: Brand = Brand("Company C")

  val tests: mutable.ArrayBuffer[Test] = mutable.ArrayBuffer(
    Test(
      testId = testId1,
      brand = brand1,
      testName = TestName("test1"),
      adVariants = LazyList(
        AdVariant(UUID.fromString("637497c4-a275-4ce0-9c2c-80624656a36a"), "Text1"),
        AdVariant(UUID.fromString("e3b559b3-eda1-432e-ae11-13d26fee4e65"), "Text2"),
        AdVariant(UUID.fromString("5adec0e3-a13b-4513-9ffa-0d5fca868c6d"), "Text3"),
        AdVariant(UUID.fromString("48d125cc-6402-48d1-b745-ea9b99bfab5b"), "Text4")
      ),
      testDuration = 105.50,
      testStatus = Live
    ),
    Test(
      testId = testId2,
      brand = brand1,
      testName = TestName("test2"),
      adVariants = LazyList(
        AdVariant(UUID.fromString("637497c4-a275-4ce0-9c2c-80624656a36a"), "Text1"),
        AdVariant(UUID.fromString("e3b559b3-eda1-432e-ae11-13d26fee4e65"), "Text2"),
        AdVariant(UUID.fromString("5adec0e3-a13b-4513-9ffa-0d5fca868c6d"), "Text3"),
        AdVariant(UUID.fromString("48d125cc-6402-48d1-b745-ea9b99bfab5b"), "Text4")
      ),
      testDuration = 193.50,
      testStatus = Pending
    ),
    Test(
      testId = testId3,
      brand = brand2,
      testName = TestName("test3"),
      adVariants = LazyList(
        AdVariant(UUID.fromString("981cad7f-a8f3-426a-8a1e-6ff322c0b6f2"), "Text1"),
        AdVariant(UUID.fromString("ffc656dc-ee19-4d3e-883c-ff0625f94f09"), "Text2")
      ),
      testDuration = 85.50,
      testStatus = Ended
    ),
    Test(
      testId = testId4,
      brand = brand3,
      testName = TestName("test4"),
      adVariants = LazyList(
        AdVariant(UUID.fromString("76fb2820-43af-4b13-af56-eccf0346ecad"), "Text1"),
        AdVariant(UUID.fromString("753dc6e5-9372-4cfd-884d-4ac67277931a"), "Text2"),
        AdVariant(UUID.fromString("51aa982b-76a3-4228-af57-6110dc004c68"), "Text2"),
        AdVariant(UUID.fromString("2d002f7a-1cf2-4238-9dc4-6efb0aafb7a3"), "Text2"),
        AdVariant(UUID.fromString("d539560b-6079-4658-b338-bd2b64fb06f8"), "Text2"),
        AdVariant(UUID.fromString("8be2f419-5ead-4395-8ff8-14afc468aed3"), "Text2")
      ),
      testDuration = 210.00,
      testStatus = Paused
    )
  )

  type IdMappedDb = Map[TestId, mutable.ArrayBuffer[Test]]
  type BrandMappedDb = Map[Brand, mutable.ArrayBuffer[Test]]

  // def, not val, so no memoization meaning GET requests to reflect updated tests
  def idMappedDb: IdMappedDb = tests.groupBy(_.testId)
  def brandMappedDb: BrandMappedDb = tests.groupBy(_.brand)
}
