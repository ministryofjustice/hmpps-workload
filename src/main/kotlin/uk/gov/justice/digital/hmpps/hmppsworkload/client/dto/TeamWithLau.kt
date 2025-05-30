package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

data class TeamWithLau constructor(
  val code: String,
  val description: String,
  val localAdminUnit: Lau,
)

data class Lau constructor(
  val code: String,
  val description: String,
  val probationDeliveryUnit: Pdu,
)

data class Pdu constructor(
  val code: String,
  val description: String,
  val provider: Provider,
)

data class Provider constructor(
  val code: String,
  val description: String,
)
