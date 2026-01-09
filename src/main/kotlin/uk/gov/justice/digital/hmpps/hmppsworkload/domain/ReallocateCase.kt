package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator

data class ReallocateCase @JsonCreator constructor(
  val crn: String,
  val emailTo: List<String>? = null,
  val sensitiveNotes: Boolean?,
  val reallocationNotes: String?,
  val laoCase: Boolean,
  val allocationReason: AllocationReason? = AllocationReason.INITIAL_ALLOCATION,
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
