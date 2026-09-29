package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

data class TeamsResponse(
  val teams: Map<String, List<StaffMember>>,
)
