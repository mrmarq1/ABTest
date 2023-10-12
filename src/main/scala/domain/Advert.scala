package domain

import java.util.UUID

object Advert {
  case class AdVariant(variantid: UUID, adtext: String, variantSpend: BigDecimal, variantDropped: Boolean)
}
