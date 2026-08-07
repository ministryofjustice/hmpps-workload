package uk.gov.justice.digital.hmpps.hmppsworkload.integration.listener

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReferenceType
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.TierApiExtension.Companion.hmppsTier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.WorkforceAllocationsToDeliusExtension.Companion.workforceAllocationsToDelius
import uk.gov.justice.digital.hmpps.hmppsworkload.listener.TierCalculationEventListener.CalculationEventData

class TierCalculationEventListenerTest : IntegrationTestBase() {

  @Test
  fun `saves updated tiers`() {
    val crn = "J678910"
    workforceAllocationsToDelius.personResponseByCrn(crn)
    hmppsTier.tierCalculationResponse(crn)
    hmppsDomainSnsClient.publish(
      PublishRequest.builder().topicArn(hmppsDomainTopicArn).message(objectMapper.writeValueAsString(tierCalculationEvent(crn)))
        .messageAttributes(
          mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue("TIER_CALCULATION_COMPLETE").build()),
        ).build(),
    )

    await untilCallTo {
      caseDetailsRepository.count()
    } matches { it!! > 0 }

    val caseDetail = caseDetailsRepository.findAll().first()

    Assertions.assertEquals(crn, caseDetail.crn)
    Assertions.assertEquals(CaseType.CUSTODY, caseDetail.type)
    Assertions.assertEquals(Tier.B, caseDetail.tier)
  }

  private fun tierCalculationEvent(crn: String) = CalculationEventData(
    PersonReference(listOf(PersonReferenceType("CRN", crn))),
  )
}
