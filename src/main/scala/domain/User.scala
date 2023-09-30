package domain

import io.estatico.newtype.macros.newtype

import java.util.UUID

object User {
  @newtype
  case class UserId(userId: UUID)

  @newtype
  case class UserName(userName: String)

  @newtype
  case class UserPassword(userPassword: String)
}
