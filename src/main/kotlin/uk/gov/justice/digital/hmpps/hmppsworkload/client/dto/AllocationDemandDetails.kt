package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigInteger
import java.time.LocalDate
import java.time.ZonedDateTime

data class AllocationDemandDetails @JsonCreator constructor(
  val crn: String,
  val name: Name,
  val staff: StaffMember,
  val allocatingStaff: StaffMember,
  val initialAppointment: InitialAppointment?,
  val sentence: SentenceDetails,
  val court: Court,
  val offences: List<OffenceDetails>,
  val activeRequirements: List<Requirement>,
)

data class SentenceDetails(
  val description: String,
  val date: ZonedDateTime,
  val length: String,
)

data class OffenceDetails(
  val mainCategory: String,
)

data class Court(
  val name: String,
  val appearanceDate: LocalDate,
)

data class Requirement(
  val mainCategory: String,
  val subCategory: String?,
  val length: String,
  val id: BigInteger,
  val manager: Manager,
  val isUnpaidWork: Boolean,
)

data class Manager(
  val code: String,
  val teamCode: String,
  val grade: String?,
  val name: Name,
  val allocated: Boolean,
)
