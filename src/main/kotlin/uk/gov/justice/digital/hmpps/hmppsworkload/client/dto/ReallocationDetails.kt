package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

data class ReallocationDetails(
  val reason: String,
  val oasysLastUpdated: String?,
  val nextAppointment: String?,
  val failureToComply: String?,
  val previouslyManagedBy: StaffMember,
  val requirements: List<Requirement>,
  val offences: List<OffenceDetails>,
  val sentences: List<SentenceDetails>,
)
