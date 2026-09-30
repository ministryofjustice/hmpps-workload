package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

import com.fasterxml.jackson.annotation.JsonCreator

data class TeamOverview @JsonCreator constructor(
  val code: String,
  val name: String,
)
