package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

data class Team(
  val code: String,
  val description: String,
  val localAdminUnit: LocalAdminUnit,
)

data class LocalAdminUnit(
  val code: String,
  val description: String,
  val probationDeliveryUnit: ProbationDeliveryUnit,
)

data class ProbationDeliveryUnit(
  val code: String,
  val description: String,
  val provider: Provider,
)

data class Provider(
  val code: String,
  val description: String,
)
