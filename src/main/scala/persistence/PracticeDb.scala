package persistence

import domain.ABTest._
import domain.Advert._
import domain.Brand._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.collection.mutable


object PracticeDb {
  val testId1: TestId = TestId(UUID.fromString("0c50d92b-3a3c-45fa-bd1b-08f4b1e9dcac"))
  val testId2: TestId = TestId(UUID.fromString("811c79f4-8e94-4721-9932-4d7a889c0261"))
  val testId3: TestId = TestId(UUID.fromString("75402ece-cf57-43e0-b534-296c44188e47"))
  val testId4: TestId = TestId(UUID.fromString("af591b46-c48a-4f8d-9f1d-863ec558ad68"))

  val brand1: BrandName = BrandName("Company A")
  val brand2: BrandName = BrandName("Company B")
  val brand3: BrandName = BrandName("Company C")

  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
  val emptyDateTime = LocalDateTime.MIN

  var testsDb: mutable.ArrayBuffer[Test] = mutable.ArrayBuffer(
    Test(
      testId = testId1,
      brandName = brand1,
      testName = TestName("test1"),
      adVariants = Vector(
        AdVariant(UUID.fromString("637497c4-a275-4ce0-9c2c-80624656a36a"), "Text1", 50000.00, false),
        AdVariant(UUID.fromString("e3b559b3-eda1-432e-ae11-13d26fee4e65"), "Text2", 270000.00, false),
        AdVariant(UUID.fromString("5adec0e3-a13b-4513-9ffa-0d5fca868c6d"), "Text3", 0, true),
        AdVariant(UUID.fromString("48d125cc-6402-48d1-b745-ea9b99bfab5b"), "Text4", 170000.50, false)
      ),
      testSpend = 1000000,
      testDuration =1405.50,
      testSubmissionDate = LocalDateTime.parse("2023-09-20 09:45", formatter),
      testStartDate = LocalDateTime.parse("2023-09-20 09:47", formatter),
      testStatus = Live,
      testUpdate = NoUpdate
    ),
    Test(
      testId = testId2,
      brandName = brand1,
      testName = TestName("test2"),
      adVariants = Vector(
        AdVariant(UUID.fromString("637497c4-a275-4ce0-9c2c-80624656a36a"), "Text1", 250000, false),
        AdVariant(UUID.fromString("e3b559b3-eda1-432e-ae11-13d26fee4e65"), "Text2", 250000, false),
        AdVariant(UUID.fromString("5adec0e3-a13b-4513-9ffa-0d5fca868c6d"), "Text3", 250000, false),
        AdVariant(UUID.fromString("48d125cc-6402-48d1-b745-ea9b99bfab5b"), "Text4", 250000, false),
      ),
      testSpend = 2500000,
      testDuration = 193.50,
      testSubmissionDate = LocalDateTime.parse("2023-02-10 10:15", formatter),
      testStartDate = emptyDateTime,
      testStatus = Pending,
      testUpdate = TestNameUpdated
    ),
    Test(
      testId = testId3,
      brandName = brand2,
      testName = TestName("test3"),
      adVariants = Vector(
        AdVariant(UUID.fromString("981cad7f-a8f3-426a-8a1e-6ff322c0b6f2"), "Text1", 350000, false),
        AdVariant(UUID.fromString("ffc656dc-ee19-4d3e-883c-ff0625f94f09"), "Text2", 0, true),
      ),
      testSpend = 500000,
      testDuration = 945.50,
      testSubmissionDate = LocalDateTime.parse("2023-01-07 13:57", formatter),
      testStartDate = LocalDateTime.parse("2023-02-15 23:27", formatter),
      testStatus = Ended,
      testUpdate = NoUpdate
    ),
    Test(
      testId = testId4,
      brandName = brand3,
      testName = TestName("test4"),
      adVariants = Vector(
        AdVariant(UUID.fromString("76fb2820-43af-4b13-af56-eccf0346ecad"), "Text1", 50000, false),
        AdVariant(UUID.fromString("753dc6e5-9372-4cfd-884d-4ac67277931a"), "Text1", 0, true),
        AdVariant(UUID.fromString("51aa982b-76a3-4228-af57-6110dc004c68"), "Text1", 20000, false),
        AdVariant(UUID.fromString("2d002f7a-1cf2-4238-9dc4-6efb0aafb7a3"), "Text1", 110000, false),
        AdVariant(UUID.fromString("d539560b-6079-4658-b338-bd2b64fb06f8"), "Text1", 160000, false),
        AdVariant(UUID.fromString("8be2f419-5ead-4395-8ff8-14afc468aed3"), "Text1", 0, true),
      ),
      testSpend = 750000,
      testDuration = 210.00,
      testSubmissionDate = LocalDateTime.parse("2023-09-30 11:01", formatter),
      testStartDate = LocalDateTime.parse("2023-10-01 19:33", formatter),
      testStatus = Paused,
      testUpdate = BrandNameUpdated
    )
  )

  type IdMappedDb = Map[TestId, mutable.ArrayBuffer[Test]]
  type BrandMappedDb = Map[BrandName, mutable.ArrayBuffer[Test]]

  // def, not val, so no memoization meaning GET requests to reflect updated tests
  def idMappedDb(): IdMappedDb = testsDb.groupBy(_.testId)
  def brandMappedDb(): BrandMappedDb = testsDb.groupBy(_.brandName)
}
