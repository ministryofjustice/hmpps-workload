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
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.TierApiExtension.Companion.hmppsTier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.WorkforceAllocationsToDeliusExtension.Companion.workforceAllocationsToDelius
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WorkloadCalculationEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.listener.WorkloadPrisonerEvent
import java.math.BigDecimal
import java.time.LocalDateTime

class WorkloadPrisonerListenerTests : IntegrationTestBase() {

  @Test
  fun `calculate workload for managed prisoner`() {
    val crn = "J678910"
    val nomsNumber = "X1111XX"
    val staffCode = "staff1"
    val teamCode = "team1"
    val availableHours = BigDecimal.valueOf(37)
    val caseDetailsEntity = CaseDetailsEntity(crn, Tier.C3, CaseType.COMMUNITY, "Jane", "Doe")

    caseDetailsRepository.save(caseDetailsEntity)
    workforceAllocationsToDelius.officerViewResponse(staffCode)
    workforceAllocationsToDelius.personResponseByNoms(nomsNumber, crn, CaseType.COMMUNITY)
    hmppsTier.tierCalculationResponse(crn)

    personManagerRepository.save(PersonManagerEntity(crn = crn, staffCode = staffCode, teamCode = teamCode, createdBy = "createdby", isActive = true, allocationReason = AllocationReason.INITIAL_ALLOCATION))

    placePrisonerReleasedMessageOnDomainTopic(nomsNumber)

    noMessagesOnWorkloadPrisonerQueue()
    noMessagesOnWorkloadPrisonerDLQ()

    val actualWorkloadCalcEntity: WorkloadCalculationEntity? =
      workloadCalculationRepository.findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)

    Assertions.assertAll(
      { Assertions.assertEquals(staffCode, actualWorkloadCalcEntity?.staffCode) },
      { Assertions.assertEquals(teamCode, actualWorkloadCalcEntity?.teamCode) },
      { Assertions.assertEquals(availableHours, actualWorkloadCalcEntity?.breakdownData?.availableHours) },
      { Assertions.assertEquals(LocalDateTime.now().dayOfMonth, actualWorkloadCalcEntity?.calculatedDate?.dayOfMonth) },
    )
  }

  private fun placePrisonerReleasedMessageOnDomainTopic(nomsNumber: String) {
    hmppsDomainSnsClient.publish(
      PublishRequest.builder().topicArn(hmppsDomainTopicArn).message(jsonString(prisonerEvent(nomsNumber)))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue("prison-offender-events.prisoner.released").build(),
          ),
        ).build(),
    )
  }

  @Test
  fun `process prisoner who is unknown to workload`() {
    val crn = "J678910"
    val nomsNumber = "X1111XX"
    workforceAllocationsToDelius.personResponseByNoms(nomsNumber, crn)
    placePrisonerReleasedMessageOnDomainTopic(nomsNumber)

    noMessagesOnWorkloadPrisonerQueue()
    noMessagesOnWorkloadPrisonerDLQ()
  }

  @Test
  fun `process prisoner not in Delius yet`() {
    val nomsNumber = "X1111XX"
    workforceAllocationsToDelius.notFoundPersonResourceResponse(nomsNumber, "NOMS")
    placePrisonerReleasedMessageOnDomainTopic(nomsNumber)

    noMessagesOnWorkloadPrisonerQueue()
    noMessagesOnWorkloadPrisonerDLQ()
  }

  private fun prisonerEvent(nomsNumber: String) = WorkloadPrisonerEvent(
    PersonReference(
      listOf(PersonReferenceType("NOMS", nomsNumber)),
    ),
  )
}
