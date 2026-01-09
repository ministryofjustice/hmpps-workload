package uk.gov.justice.digital.hmpps.hmppsworkload.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.RequirementManagerEntity
import java.time.LocalDateTime

private const val CRN = "crn"

private const val CASE_TYPE = "caseType"

private const val TEAM_CODE = "teamCode"

private const val STAFF_CODE = "staffCode"

private const val EVENT_NUMBER = "eventNumber"

@Component
class TelemetryService(@Autowired private val telemetryClient: TelemetryClient) {

  fun trackPersonManagerAllocated(personManagerEntity: PersonManagerEntity, caseType: String?) {
    trackEvent(
      TelemetryEventType.PERSON_MANAGER_ALLOCATED,
      mapOf(
        CRN to personManagerEntity.crn,
        CASE_TYPE to caseType,
        TEAM_CODE to personManagerEntity.teamCode,
        STAFF_CODE to personManagerEntity.staffCode,
        "wmtPeriod" to getWmtPeriod(LocalDateTime.now()),
      ),
    )
  }

  fun trackEventManagerAllocated(eventManagerEntity: EventManagerEntity, caseType: String?) {
    trackEvent(
      TelemetryEventType.EVENT_MANAGER_ALLOCATED,
      mapOf(
        CRN to eventManagerEntity.crn,
        CASE_TYPE to caseType,
        TEAM_CODE to eventManagerEntity.teamCode,
        // allocatedTeamCode is a temporary field while improving metrics from hmpps-allocations
        "allocatedTeamCode" to eventManagerEntity.teamCode,
        STAFF_CODE to eventManagerEntity.staffCode,
        EVENT_NUMBER to eventManagerEntity.eventNumber.toString(10),
      ),
    )
  }

  fun trackRequirementManagerAllocated(requirementManagerEntity: RequirementManagerEntity, caseType: String?) {
    trackEvent(
      TelemetryEventType.REQUIREMENT_MANAGER_ALLOCATED,
      mapOf(
        CRN to requirementManagerEntity.crn,
        CASE_TYPE to caseType,
        TEAM_CODE to requirementManagerEntity.teamCode,
        STAFF_CODE to requirementManagerEntity.staffCode,
        EVENT_NUMBER to requirementManagerEntity.eventNumber.toString(10),
        "requirementId" to requirementManagerEntity.requirementId.toString(10),
      ),
    )
  }

  fun trackStaffGradeToTierAllocated(tier: String?, deliusStaff: StaffMember, teamCode: String) {
    trackEvent(
      TelemetryEventType.STAFF_GRADE_TIER_ALLOCATED,
      mapOf(
        TEAM_CODE to teamCode,
        "staffGrade" to deliusStaff.getGrade(),
        "tier" to tier,
      ),
    )
  }

  private fun trackEvent(eventType: TelemetryEventType, customDimensions: Map<String, String?>) {
    telemetryClient.trackEvent(eventType.eventName, customDimensions, null)
  }
}
