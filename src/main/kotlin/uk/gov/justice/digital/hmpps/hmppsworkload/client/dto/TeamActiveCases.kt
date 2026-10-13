package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

import com.fasterxml.jackson.annotation.JsonCreator

data class TeamActiveCases @JsonCreator constructor(
  val code: String,
  val description: String,
  val staff: List<StaffActiveCases>,
)
