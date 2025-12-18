package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator

data class AllocateCase @JsonCreator constructor(
  val crn: String,
  val instructions: String = "",
  val emailTo: List<String>? = null,
  val sendEmailCopyToAllocatingOfficer: Boolean,
  val eventNumber: Int,
  val allocationJustificationNotes: String?,
  val sensitiveNotes: Boolean?,
  val spoOversightNotes: String?,
  val sensitiveOversightNotes: Boolean?,
  val laoCase: Boolean,
  val allocationReason: AllocationReason?,
  val nextAppointmentDate: String?,
  val lastOasysAssessmentDate: String?,
  val failureToComply: String?,
)

enum class AllocationReason {
  ALLOCATED_TO_RESPONSIBLE_OFFICER,
  CASELOAD_ADJUSTMENT,
  CHANGE_IN_TIER_OR_RISK,
  CHANGE_OF_ADDRESS,
  OFFICER_LEFT,
  OTHER,
  RISK_TO_STAFF,
  TRANSFER_IN,
  PROBATION_RESET,
  INITIAL_ALLOCATION,
}
