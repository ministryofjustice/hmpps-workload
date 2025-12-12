package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

data class ReallocationDetails(
  val reason: String,
  val oasysLastUpdated: String?,
  val nextAppointment: String?,
  val failureToComply: String?,
  val previouslyManagedBy: StaffMember,
)
