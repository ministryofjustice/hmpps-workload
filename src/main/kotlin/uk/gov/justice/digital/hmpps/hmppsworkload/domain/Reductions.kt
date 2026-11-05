package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import java.time.LocalDate

data class Reductions(
  val reductionHours: Double,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val reductionType: String,
  val reductionStatus: String,
  val contractedHours: Int,
)
