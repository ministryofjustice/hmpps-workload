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
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.TierApiExtension.Companion.hmppsTier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.WorkforceAllocationsToDeliusExtension.Companion.workforceAllocationsToDelius

class OffenderDetailsEventListenerTests : IntegrationTestBase() {

  @Test
  fun `must save case details when processing new sentence event`() {
    val crn = "J678910"

    workforceAllocationsToDelius.personResponseByCrn(crn)
    hmppsTier.tierCalculationResponse(crn)

    hmppsOffenderSnsClient.publish(
      PublishRequest.builder().topicArn(hmppsOffenderTopicArn).message(jsonString(offenderEvent(crn))).messageAttributes(
        mapOf("eventType" to MessageAttributeValue.builder().dataType("String").stringValue("OFFENDER_DETAILS_CHANGED").build()),
      ).build(),
    )

    await untilCallTo {
      caseDetailsRepository.count()
    } matches { it!! > 0 }

    val caseDetail = caseDetailsRepository.findAll().first()

    hmppsTier.verifyTierCalled(crn, 1)
    Assertions.assertEquals(crn, caseDetail.crn)
    Assertions.assertEquals(CaseType.CUSTODY, caseDetail.type)
    Assertions.assertEquals(Tier.B, caseDetail.tier)
    Assertions.assertEquals("Jane", caseDetail.firstName)
    Assertions.assertEquals("Doe", caseDetail.surname)
  }
}
