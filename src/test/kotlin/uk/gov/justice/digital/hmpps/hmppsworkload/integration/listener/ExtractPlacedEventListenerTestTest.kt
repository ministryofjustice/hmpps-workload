package uk.gov.justice.digital.hmpps.hmppsworkload.integration.listener

import com.amazonaws.services.sqs.model.SendMessageRequest
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.domain.WMTStaff
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.ReductionCategoryEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.ReductionEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.ReductionReasonEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.ReductionStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

class ExtractPlacedEventListenerTestTest : IntegrationTestBase() {

  lateinit var wmtStaff: WMTStaff
  lateinit var reductionReason: ReductionReasonEntity

  @BeforeEach
  fun setupReductionTestData() {
    val reductionCategory = reductionCategoryRepository.save(ReductionCategoryEntity())
    reductionReason = reductionReasonRepository.save(ReductionReasonEntity(reductionCategoryEntity = reductionCategory))

    wmtStaff = setupCurrentWmtStaff("STAFF2", "TEAM2")
  }

  @Test
  fun `must listen to extract placed event`() {
    hmppsExtractPlacedClient.sendMessage(SendMessageRequest(hmppsExtractPlacedQueue.queueUrl, "{}"))

    noMessagesOnExtractPlacedQueue()
    noMessagesOnExtractPlacedDLQ()
  }

  @Test
  fun `can update incorrect Active reduction to Archived`() {
    val activeReductionWhichIsNowArchived = reductionsRepository.save(ReductionEntity(workloadOwner = wmtStaff.wmtWorkloadOwnerEntity, hours = BigDecimal.valueOf(3.2), effectiveFrom = ZonedDateTime.now().minusDays(2), effectiveTo = ZonedDateTime.now().minusDays(1), status = ReductionStatus.ACTIVE, reductionReasonId = reductionReason.id!!))
    val activeReduction = reductionsRepository.save(ReductionEntity(workloadOwner = wmtStaff.wmtWorkloadOwnerEntity, hours = BigDecimal.valueOf(5), effectiveFrom = ZonedDateTime.now().minusDays(7), effectiveTo = ZonedDateTime.now().plusDays(7), status = ReductionStatus.ACTIVE, reductionReasonId = reductionReason.id!!))
    val deletedReductionInPast = reductionsRepository.save(ReductionEntity(workloadOwner = wmtStaff.wmtWorkloadOwnerEntity, hours = BigDecimal.valueOf(3.2), effectiveFrom = ZonedDateTime.now().minusDays(7), effectiveTo = ZonedDateTime.now().minusDays(1), status = ReductionStatus.DELETED, reductionReasonId = reductionReason.id!!))

    hmppsExtractPlacedClient.sendMessage(SendMessageRequest(hmppsExtractPlacedQueue.queueUrl, "{}"))

    noMessagesOnExtractPlacedQueue()
    Assertions.assertEquals(ReductionStatus.ARCHIVED, reductionsRepository.findByIdOrNull(activeReductionWhichIsNowArchived.id!!)?.status)

    Assertions.assertEquals(activeReduction.status, reductionsRepository.findByIdOrNull(activeReduction.id!!)?.status)
    Assertions.assertEquals(deletedReductionInPast.status, reductionsRepository.findByIdOrNull(deletedReductionInPast.id!!)?.status)
  }

  @Test
  fun `can update incorrect Scheduled reduction to be Active`() {
    val scheduledReductionWhichIsNowActive = reductionsRepository.save(ReductionEntity(workloadOwner = wmtStaff.wmtWorkloadOwnerEntity, hours = BigDecimal.valueOf(3.2), effectiveFrom = ZonedDateTime.now().minusDays(1), effectiveTo = ZonedDateTime.now().plusDays(1), status = ReductionStatus.SCHEDULED, reductionReasonId = reductionReason.id!!))
    hmppsExtractPlacedClient.sendMessage(SendMessageRequest(hmppsExtractPlacedQueue.queueUrl, "{}"))
    noMessagesOnExtractPlacedQueue()
    Assertions.assertEquals(ReductionStatus.ACTIVE, reductionsRepository.findByIdOrNull(scheduledReductionWhichIsNowActive.id!!)?.status)
  }

  @Test
  fun `do nothing when reduction status is correct`() {
    val activeReduction = reductionsRepository.save(ReductionEntity(workloadOwner = wmtStaff.wmtWorkloadOwnerEntity, hours = BigDecimal.valueOf(3.2), effectiveFrom = ZonedDateTime.now().minusDays(1), effectiveTo = ZonedDateTime.now().plusDays(1), status = ReductionStatus.ACTIVE, reductionReasonId = reductionReason.id!!))
    hmppsExtractPlacedClient.sendMessage(SendMessageRequest(hmppsExtractPlacedQueue.queueUrl, "{}"))
    noMessagesOnExtractPlacedQueue()
    Assertions.assertEquals(ReductionStatus.ACTIVE, reductionsRepository.findByIdOrNull(activeReduction.id!!)?.status)
  }

  @Test
  fun `can publish out of date reductions when updating reduction status`() {
    reductionsRepository.save(ReductionEntity(workloadOwner = wmtStaff.wmtWorkloadOwnerEntity, hours = BigDecimal.valueOf(3.2), effectiveFrom = ZonedDateTime.now().minusDays(1), effectiveTo = ZonedDateTime.now().plusDays(1), status = ReductionStatus.SCHEDULED, reductionReasonId = reductionReason.id!!))
    hmppsExtractPlacedClient.sendMessage(SendMessageRequest(hmppsExtractPlacedQueue.queueUrl, "{}"))
    noMessagesOnExtractPlacedQueue()
    await untilCallTo { verifyReductionsCompletedOnQueue() } matches { it == true }
  }

  @Test
  fun `out of date reductions message contain required data`() {
    reductionsRepository.save(ReductionEntity(workloadOwner = wmtStaff.wmtWorkloadOwnerEntity, hours = BigDecimal.valueOf(3.2), effectiveFrom = ZonedDateTime.now().minusDays(1), effectiveTo = ZonedDateTime.now().plusDays(1), status = ReductionStatus.SCHEDULED, reductionReasonId = reductionReason.id!!))
    hmppsExtractPlacedClient.sendMessage(SendMessageRequest(hmppsExtractPlacedQueue.queueUrl, "{}"))
    noMessagesOnExtractPlacedQueue()
    await untilCallTo { verifyReductionsCompletedOnQueue() } matches { it == true }

    Assertions.assertEquals("OUT_OF_DATE_REDUCTIONS", getReductionsCompletedMessages().eventType)
  }

  @Test
  fun `emit audit message when reductions change`() {
    reductionsRepository.save(ReductionEntity(workloadOwner = wmtStaff.wmtWorkloadOwnerEntity, hours = BigDecimal.valueOf(3.2), effectiveFrom = ZonedDateTime.now().minusDays(1), effectiveTo = ZonedDateTime.now().plusDays(1), status = ReductionStatus.SCHEDULED, reductionReasonId = reductionReason.id!!))
    hmppsExtractPlacedClient.sendMessage(SendMessageRequest(hmppsExtractPlacedQueue.queueUrl, "{}"))

    await untilCallTo { verifyAuditMessageOnQueue() } matches { it == true }
  }
}