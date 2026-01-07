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
)
