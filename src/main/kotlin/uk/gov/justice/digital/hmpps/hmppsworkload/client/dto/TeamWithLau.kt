package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

data class TeamWithLau constructor(
  val code: String = String(),
  val description: String = String(),
  val localAdminUnit: Lau = Lau(),
)

data class Lau constructor(
  val code: String = String(),
  val description: String = String(),
  val probationDeliveryUnit: Pdu = Pdu(),
)

data class Pdu constructor(
  val code: String = String(),
  val description: String = String(),
  val provider: Provider = Provider(),
)

data class Provider constructor(
  val code: String = String(),
  val description: String = String(),
)
