package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import java.time.LocalDate

data class Reductions(
  val hours: Double,
  val startDate: LocalDate,
  val endDate: LocalDate,
)
