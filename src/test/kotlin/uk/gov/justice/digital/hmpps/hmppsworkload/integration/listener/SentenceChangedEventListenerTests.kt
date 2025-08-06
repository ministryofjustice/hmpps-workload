package uk.gov.justice.digital.hmpps.hmppsworkload.integration.listener

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.TierApiExtension.Companion.hmppsTier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.WorkforceAllocationsToDeliusExtension.Companion.workforceAllocationsToDelius
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.RequirementManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WorkloadCalculationEntity
import java.math.BigInteger
import java.time.LocalDateTime

class SentenceChangedEventListenerTests : IntegrationTestBase() {

  @Test
  fun `case type is unknown if there is no sentence`() {
    val crn = "J678910"
    workforceAllocationsToDelius.personResponseByCrn(crn, caseType = CaseType.UNKNOWN)
    hmppsTier.tierCalculationResponse(crn)
    placeSentenceChangedEventOnOffenderTopic(crn)

    noMessagesOnOffenderEventsQueue()
    noMessagesOnOffenderEventsDLQ()
    val caseCount = caseDetailsRepository.count()

    assertEquals(0, caseCount)
  }

  private fun placeSentenceChangedEventOnOffenderTopic(crn: String) {
    hmppsOffenderSnsClient.publish(
      PublishRequest.builder().topicArn(hmppsOffenderTopicArn).message(jsonString(offenderEvent(crn)))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue("SENTENCE_CHANGED").build(),
          ),
        ).build(),
    ).get()
  }

  @Test
  fun `do not write case details if there is no tier`() {
    val crn = "J678910"
    workforceAllocationsToDelius.personResponseByCrn(crn)
    hmppsTier.tierCalculationNotFoundResponse(crn)
    placeSentenceChangedEventOnOffenderTopic(crn)

    noMessagesOnOffenderEventsQueue()
    noMessagesOnOffenderEventsDLQ()
    val caseCount = caseDetailsRepository.count()

    assertEquals(0, caseCount)
  }

  @Test
  fun `must save case details when processing new sentence event`() {
    val crn = "J678910"
    val staffCode = "staff1"
    val teamCode = "team1"

    hmppsTier.tierCalculationResponse(crn)
    workforceAllocationsToDelius.personResponseByCrn(crn)
    personManagerRepository.save(PersonManagerEntity(crn = crn, staffCode = staffCode, teamCode = teamCode, createdBy = "createdby", isActive = true))

    placeSentenceChangedEventOnOffenderTopic(crn)

    await untilCallTo {
      caseDetailsRepository.count()
    } matches { it!! > 0 }

    val caseDetail = caseDetailsRepository.findAll().first()

    assertEquals(crn, caseDetail.crn)
    assertEquals(CaseType.CUSTODY, caseDetail.type)
    assertEquals(Tier.B3, caseDetail.tier)
    assertEquals("Jane", caseDetail.firstName)
    assertEquals("Doe", caseDetail.surname)
  }

  @Test
  fun `only save latest case details if they have changed`() {
    val crn = "J678910"
    val staffCode = "staff1"
    val teamCode = "team1"

    workforceAllocationsToDelius.personResponseByCrn(crn)
    hmppsTier.tierCalculationResponse(crn)

    workforceAllocationsToDelius.personResponseByCrn(crn)
    hmppsTier.tierCalculationResponse(crn, Tier.C3.name)

    personManagerRepository.save(PersonManagerEntity(crn = crn, staffCode = staffCode, teamCode = teamCode, createdBy = "createdby", isActive = true))

    placeSentenceChangedEventOnOffenderTopic(crn)
    placeSentenceChangedEventOnOffenderTopic(crn)

    noMessagesOnOffenderEventsQueue()

    val count = caseDetailsRepository.count()

    assertEquals(1, count)

    val caseDetail = caseDetailsRepository.findByIdOrNull(crn)!!
    assertEquals(Tier.C3, caseDetail.tier)
  }

  @Test
  fun `case details not saved if no active convictions exist`() {
    val crn = "J678910"
    workforceAllocationsToDelius.personResponseByCrn(crn)

    placeSentenceChangedEventOnOffenderTopic(crn)

    noMessagesOnOffenderEventsQueue()
    noMessagesOnOffenderEventsDLQ()

    assertEquals(0, caseDetailsRepository.count())
  }

  @Test
  fun `case details deleted if no active convictions exist`() {
    val crn = "J678910"
    caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.C1, CaseType.COMMUNITY, "Jane", "Doe"))

    val personManagerEntity = personManagerRepository.save(PersonManagerEntity(crn = crn, staffCode = "STFFCDE", teamCode = "TM1", createdBy = "USER1", isActive = true))
    val eventManagerEntity = eventManagerRepository.save(
      EventManagerEntity(
        crn = crn,
        staffCode = "STFFCDE",
        teamCode = "TM1",
        createdBy = "USER1",
        isActive = true,
        eventNumber = 1,
        spoStaffCode = "SP2",
        spoName = "Fred flintstone",
      ),
    )
    val requirementManagerEntity = requirementManagerRepository.save(
      RequirementManagerEntity(
        crn = crn,
        requirementId = BigInteger.TWO,
        staffCode = "STFFCDE",
        teamCode = "TM1",
        createdBy = "USER1",
        isActive = true,
        eventNumber = 1,
      ),
    )

    workforceAllocationsToDelius.personResponseByCrn(crn)

    placeSentenceChangedEventOnOffenderTopic(crn)

    noMessagesOnNotificationQueue()
    noMessagesOnNotificationQueueDLQ()
    noMessagesOnOffenderEventsQueue()
    noMessagesOnOffenderEventsDLQ()

    val count = caseDetailsRepository.count()

    // check that the case details have been deleted
    assertEquals(0, count)
    assertFalse(personManagerRepository.findByIdOrNull(personManagerEntity.id!!)!!.isActive)
    assertFalse(eventManagerRepository.findByIdOrNull(eventManagerEntity.id!!)!!.isActive)
    assertFalse(requirementManagerRepository.findByIdOrNull(requirementManagerEntity.id!!)!!.isActive)
  }

  @Test
  fun `update case details when there is a change`() {
    val crn = "J678910"
    val staffCode = "staff1"
    val teamCode = "team1"

    workforceAllocationsToDelius.personResponseByCrn(crn)
    hmppsTier.tierCalculationResponse(crn)

    val caseDetailsEntity = CaseDetailsEntity(crn, Tier.C3, CaseType.COMMUNITY, "Jane", "Doe")
    personManagerRepository.save(PersonManagerEntity(crn = crn, staffCode = staffCode, teamCode = teamCode, createdBy = "createdby", isActive = true))
    caseDetailsRepository.save(caseDetailsEntity)

    placeSentenceChangedEventOnOffenderTopic(crn)

    noMessagesOnOffenderEventsQueue()

    assertThat(caseDetailsRepository.count()).isEqualTo(1)
    assertThat(caseDetailsRepository.findByIdOrNull(crn)?.tier).isEqualTo(Tier.B3)
  }

  @Test
  fun `calculate workload when updating case details for realtime offender manager`() {
    val crn = "J678910"
    val staffCode = "staff1"
    val teamCode = "team1"

    workforceAllocationsToDelius.personResponseByCrn(crn)
    hmppsTier.tierCalculationResponse(crn)

    val caseDetailsEntity = CaseDetailsEntity(crn, Tier.C3, CaseType.COMMUNITY, "Jane", "Doe")

    caseDetailsRepository.save(caseDetailsEntity)

    workforceAllocationsToDelius.officerViewResponse(staffCode)
    personManagerRepository.save(PersonManagerEntity(crn = crn, staffCode = staffCode, teamCode = teamCode, createdBy = "createdby", isActive = true))

    placeSentenceChangedEventOnOffenderTopic(crn)

    noMessagesOnOffenderEventsQueue()

    val actualWorkloadCalcEntity: WorkloadCalculationEntity? =
      workloadCalculationRepository.findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)

    Assertions.assertAll(
      { assertEquals(staffCode, actualWorkloadCalcEntity?.staffCode) },
      { assertEquals(teamCode, actualWorkloadCalcEntity?.teamCode) },
      { assertEquals(LocalDateTime.now().dayOfMonth, actualWorkloadCalcEntity?.calculatedDate?.dayOfMonth) },
    )
  }
}
