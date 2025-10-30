package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDate

data class StaffActiveCases @JsonCreator constructor(
  val code: String,
  val name: Name,
  private val grade: String?,
  val email: String?,
  val cases: List<ActiveCase>,
) : Grade(grade)

data class ActiveCase(
  val crn: String,
  val name: Name,
  val type: String,
  val initialAllocationDate: LocalDate?,
)
