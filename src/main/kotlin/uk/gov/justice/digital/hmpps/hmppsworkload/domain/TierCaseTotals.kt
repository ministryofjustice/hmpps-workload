package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import java.math.BigDecimal

data class TierCaseTotals(
  val A: BigDecimal = BigDecimal.ZERO,
  val B: BigDecimal = BigDecimal.ZERO,
  val C: BigDecimal = BigDecimal.ZERO,
  val D: BigDecimal = BigDecimal.ZERO,
  val E: BigDecimal = BigDecimal.ZERO,
  val F: BigDecimal = BigDecimal.ZERO,
  val G: BigDecimal = BigDecimal.ZERO,
  val missing: BigDecimal = BigDecimal.ZERO,
  val notSupervised: BigDecimal = BigDecimal.ZERO,
)
