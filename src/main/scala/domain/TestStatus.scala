package domain

sealed trait TestStatus

case object Pending extends TestStatus
case object Live extends TestStatus
case object Paused extends TestStatus
case object Ended extends TestStatus
