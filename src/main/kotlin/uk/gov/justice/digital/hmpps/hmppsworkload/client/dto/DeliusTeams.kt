package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

data class DeliusTeams(
  val datasets: List<Dataset> = emptyList(),
  val teams: List<TeamWithLau> = emptyList(),
)

data class Dataset(
  val code: String = String(),
  val description: String = String(),
)
