package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

data class DeliusTeams(
  val datasets: List<Dataset>,
  val teams: List<TeamWithLau>,
)

data class Dataset(
  val code: String,
  val description: String,
)
