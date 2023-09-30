package domain

import io.estatico.newtype.macros.newtype

object Brand {
  @newtype
  case class Brand(brand: String)
}
