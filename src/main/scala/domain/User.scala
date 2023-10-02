package domain

import java.util.UUID

object User {
  case class UserId(userId: UUID)

  case class UserName(userName: String)

  case class UserPassword(userPassword: String)
}
