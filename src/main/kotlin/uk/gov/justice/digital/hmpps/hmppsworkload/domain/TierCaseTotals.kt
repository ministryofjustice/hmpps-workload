package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import java.math.BigDecimal

data class TierCaseTotals(
  val a: BigDecimal = BigDecimal.ZERO,
  val b: BigDecimal = BigDecimal.ZERO,
  val c: BigDecimal = BigDecimal.ZERO,
  val d: BigDecimal = BigDecimal.ZERO,
  val e: BigDecimal = BigDecimal.ZERO,
  val f: BigDecimal = BigDecimal.ZERO,
  val g: BigDecimal = BigDecimal.ZERO,
  val missing: BigDecimal = BigDecimal.ZERO,
  val notSupervised: BigDecimal = BigDecimal.ZERO,
)
