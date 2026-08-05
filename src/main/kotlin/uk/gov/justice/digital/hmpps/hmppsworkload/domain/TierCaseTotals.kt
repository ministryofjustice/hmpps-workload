package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import java.math.BigDecimal

data class TierCaseTotals(
  val A: BigDecimal,
  val B: BigDecimal,
  val C: BigDecimal,
  val D: BigDecimal,
  val E: BigDecimal,
  val F: BigDecimal,
  val G: BigDecimal,
  val missing: BigDecimal,
  val notSupervised: BigDecimal,
)
