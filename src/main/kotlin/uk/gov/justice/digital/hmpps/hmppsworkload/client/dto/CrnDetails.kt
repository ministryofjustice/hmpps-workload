package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto
import java.time.LocalDate

data class CrnDetails(
  val crn: String,
  val name: Name,
  val dateOfBirth: LocalDate,
  val manager: Manager,
  val hasActiveOrder: Boolean,
)
