package domain

case class NewTest(org: Org, testName: TestName, adTextVariants: LazyList[AdText], testDuration: Double)
