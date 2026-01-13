package uk.gov.justice.digital.hmpps.hmppsworkload.integration.listener

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReferenceType
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.WorkforceAllocationsToDeliusExtension.Companion.workforceAllocationsToDelius
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WorkloadCalculationEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.listener.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsworkload.listener.WorkloadCalculationEvent
import java.math.BigDecimal
import java.time.LocalDateTime

class WorkloadCalculationListenerTests : IntegrationTestBase() {

  @Test
  fun `calculate workload when staff available hours have changed`() {
    val crn = "J678910"
    val staffCode = "staff1"
    val teamCode = "team1"
    val availableHours = BigDecimal.TEN
    val caseDetailsEntity = CaseDetailsEntity(crn, Tier.C3, CaseType.COMMUNITY, "Jane", "Doe")

    caseDetailsRepository.save(caseDetailsEntity)

    workforceAllocationsToDelius.officerViewResponse(staffCode)
    personManagerRepository.save(PersonManagerEntity(crn = crn, staffCode = staffCode, teamCode = teamCode, createdBy = "createdby", isActive = true, allocationReason = AllocationReason.INITIAL_ALLOCATION))

    placeStaffAvailableHoursChangedMessageOnDomainTopic(staffCode, teamCode, availableHours)

    noMessagesOnWorkloadCalculationEventsQueue()

    val actualWorkloadCalcEntity: WorkloadCalculationEntity? =
      workloadCalculationRepository.findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)

    Assertions.assertAll(
      { Assertions.assertEquals(staffCode, actualWorkloadCalcEntity?.staffCode) },
      { Assertions.assertEquals(teamCode, actualWorkloadCalcEntity?.teamCode) },
      { Assertions.assertEquals(availableHours, actualWorkloadCalcEntity?.breakdownData?.availableHours) },
      { Assertions.assertEquals(LocalDateTime.now().dayOfMonth, actualWorkloadCalcEntity?.calculatedDate?.dayOfMonth) },
    )
  }

  private fun placeStaffAvailableHoursChangedMessageOnDomainTopic(staffCode: String, teamCode: String, availableHours: BigDecimal) {
    hmppsDomainSnsClient.publish(
      PublishRequest.builder().topicArn(hmppsDomainTopicArn)
        .message(jsonString(staffAvailableHoursChangedEvent(staffCode, teamCode, availableHours))).messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue("staff.available.hours.changed")
              .build(),
          ),
        ).build(),
    )
  }

  @Test
  fun `move event onto dlq when retrieving grade fails`() {
    val crn = "J678910"
    val staffCode = "staff1"
    val teamCode = "team1"
    val availableHours = BigDecimal.TEN
    val caseDetailsEntity = CaseDetailsEntity(crn, Tier.C3, CaseType.COMMUNITY, "Jane", "Doe")

    caseDetailsRepository.save(caseDetailsEntity)

    workforceAllocationsToDelius.officerViewErrorResponse(staffCode)
    personManagerRepository.save(PersonManagerEntity(crn = crn, staffCode = staffCode, teamCode = teamCode, createdBy = "createdby", isActive = true, allocationReason = AllocationReason.INITIAL_ALLOCATION))

    placeStaffAvailableHoursChangedMessageOnDomainTopic(staffCode, teamCode, availableHours)

    noMessagesOnWorkloadCalculationEventsQueue()

    oneMessageOnWorkloadCalculationDeadLetterQueue()
  }

  private fun staffAvailableHoursChangedEvent(staffCode: String, teamCode: String, availableHours: BigDecimal) = WorkloadCalculationEvent(
    AdditionalInformation(availableHours),
    PersonReference(listOf(PersonReferenceType("staffCode", staffCode), PersonReferenceType("teamCode", teamCode))),
  )
}
