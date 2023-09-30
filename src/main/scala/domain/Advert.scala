package domain

import io.estatico.newtype.macros.newtype

import java.util.UUID

object Advert {
  type AdText = String

  @newtype
  case class AdVariant(variantId: UUID, adText: AdText)
}
