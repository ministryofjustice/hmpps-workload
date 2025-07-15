package uk.gov.justice.digital.hmpps.hmppsworkload.integration

import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocationDemandDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Court
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.InitialAppointment
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Name
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.OffenceDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Requirement
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskOGRS
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.SentenceDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

fun getAllocationDetails(
  crn: String,
  initialAppointment: LocalDate? = null,
  offenceDetails: List<OffenceDetails> = emptyList(),
  activeRequirements: List<Requirement> = emptyList(),
  ogrs: RiskOGRS? = null,
) = AllocationDemandDetails(
  crn,
  Name(
    "Jonathon",
    "",
    "Jones",
  ),
  StaffMember(
    "STAFF1",
    Name(
      "Staff",
      "",
      "Member",
    ),
    "simulate-delivered@notifications.service.gov.uk",
    "PO",
  ),
  StaffMember(
    "STAFF2",
    Name(
      "Allocating",
      "",
      "Member",
    ),
    null,
    "SPO",
  ),
  initialAppointment?.let { InitialAppointment(initialAppointment) },
  ogrs,
  SentenceDetails(
    "CUSTODY",
    ZonedDateTime.of(LocalDate.of(2022, 10, 7), LocalTime.MIDNIGHT, ZoneId.systemDefault()),
    "6 Months",
  ),
  Court(
    "Court Name",
    LocalDate.now(),
  ),
  offenceDetails,
  activeRequirements,
)
