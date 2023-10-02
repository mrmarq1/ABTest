package domain

import java.util.UUID

object Advert {
  type variantText = String

  case class AdVariant(variantId: UUID, variantText: variantText, variantSpend: BigDecimal, variantDropped: Boolean)
}
