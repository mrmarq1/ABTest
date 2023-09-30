package domain

case class Test(testId: TestId, org: Org, testName: TestName, adVariants: LazyList[AdVariant],
                testDuration: Double, testStatus: TestStatus)

type AdText = String
