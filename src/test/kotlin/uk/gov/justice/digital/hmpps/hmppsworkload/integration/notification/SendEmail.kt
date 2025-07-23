package uk.gov.justice.digital.hmpps.hmppsworkload.integration.notification

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.reactor.asCoroutineContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import reactor.util.context.Context
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType.COMMUNITY
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier.B3
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.getAllocationDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.AssessRisksNeedsApiExtension.Companion.assessRisksNeedsApi
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.WorkforceAllocationsToDeliusExtension.Companion.workforceAllocationsToDelius
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.service.NotificationService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.SqsSuccessPublisher
import java.util.*

class SendEmail : IntegrationTestBase() {

  @Autowired
  lateinit var notificationService: NotificationService
  private val sqsSuccessPublisher = mockk<SqsSuccessPublisher>()

  @BeforeEach
  fun setUp() {
    coEvery { sqsSuccessPublisher.sendNotification(any()) } returns Unit
  }

  @Test
  fun `sends an email when ROSH cannot be retrieved`() = runBlocking(Context.of(HttpHeaders.AUTHORIZATION, "token").asCoroutineContext()) {
    val crn = "X123456"
    val allocateCase = AllocateCase(crn, sendEmailCopyToAllocatingOfficer = false, eventNumber = 1, allocationJustificationNotes = "some notes", sensitiveNotes = false, spoOversightNotes = "spo notes", sensitiveOversightNotes = null, laoCase = false)
    val allocationDetails = getAllocationDetails(crn)
    val caseDetailsEntity = CaseDetailsEntity(crn, B3, COMMUNITY, "Jane", "Doe")

    assessRisksNeedsApi.riskSummaryErrorResponse(crn)
    assessRisksNeedsApi.riskPredictorResponse(crn)
    workforceAllocationsToDelius.getDeliusAllowedTeamsResponse(allocationDetails.allocatingStaff.code)
    workforceAllocationsToDelius.getDeliusAllowedTeamsResponse(allocationDetails.staff.code)
    coEvery { sqsSuccessPublisher.sendNotification(any()) } returns Unit

    caseDetailsRepository.save(caseDetailsEntity)
    val emailMessageResponse = notificationService.notifyAllocation(
      allocationDetails,
      allocateCase,
      caseDetailsEntity,
    )

    assertEquals("5db23c80-9cb6-4b8e-a0f6-56061e50a9ef", emailMessageResponse.templateId)
  }

  @Test
  fun `sends an lao email when ROSH cannot be retrieved for lao case`() = runBlocking(Context.of(HttpHeaders.AUTHORIZATION, "token").asCoroutineContext()) {
    val crn = "X123456"
    val allocateCase = AllocateCase(crn, sendEmailCopyToAllocatingOfficer = false, eventNumber = 1, allocationJustificationNotes = "some notes", sensitiveNotes = false, spoOversightNotes = "spo notes", sensitiveOversightNotes = null, laoCase = true)
    val allocationDetails = getAllocationDetails(crn)
    val caseDetailsEntity = CaseDetailsEntity(crn, B3, COMMUNITY, "Jane", "Doe")

    assessRisksNeedsApi.riskSummaryErrorResponse(crn)
    assessRisksNeedsApi.riskPredictorResponse(crn)
    workforceAllocationsToDelius.getDeliusAllowedTeamsResponse(allocationDetails.allocatingStaff.code)
    workforceAllocationsToDelius.getDeliusAllowedTeamsResponse(allocationDetails.staff.code)
    caseDetailsRepository.save(caseDetailsEntity)
    val emailMessageResponse = notificationService.notifyAllocation(
      allocationDetails,
      allocateCase,
      caseDetailsEntity,
    )
    assertEquals("fc55e1ce-47d6-479c-ac80-3ac77c9fe609", emailMessageResponse.templateId)
  }

  @Test
  fun `sends an email when risk predictor cannot be retrieved`() = runBlocking(Context.of(HttpHeaders.AUTHORIZATION, "token").asCoroutineContext()) {
    val crn = "X123456"
    val allocateCase = AllocateCase(crn, sendEmailCopyToAllocatingOfficer = false, eventNumber = 1, allocationJustificationNotes = "some notes", sensitiveNotes = false, spoOversightNotes = "spo notes", sensitiveOversightNotes = null, laoCase = false)
    val allocationDetails = getAllocationDetails(crn)
    val caseDetailsEntity = CaseDetailsEntity(crn, B3, COMMUNITY, "Jane", "Doe")

    assessRisksNeedsApi.riskSummaryResponse(crn)
    assessRisksNeedsApi.riskPredictorErrorResponse(crn)
    workforceAllocationsToDelius.getDeliusAllowedTeamsResponse(allocationDetails.allocatingStaff.code)
    workforceAllocationsToDelius.getDeliusAllowedTeamsResponse(allocationDetails.staff.code)
    caseDetailsRepository.save(caseDetailsEntity)
    val emailMessageResponse = notificationService.notifyAllocation(
      allocationDetails,
      allocateCase,
      caseDetailsEntity,
    )

    assertEquals("5db23c80-9cb6-4b8e-a0f6-56061e50a9ef", emailMessageResponse.templateId)
  }

  @Test
  fun `sends an lao email when risk predictor cannot be retrieved for lao case`() = runBlocking(Context.of(HttpHeaders.AUTHORIZATION, "token").asCoroutineContext()) {
    val crn = "X123456"
    val allocateCase = AllocateCase(crn, sendEmailCopyToAllocatingOfficer = false, eventNumber = 1, allocationJustificationNotes = "some notes", sensitiveNotes = false, spoOversightNotes = "spo notes", sensitiveOversightNotes = null, laoCase = true)
    val allocationDetails = getAllocationDetails(crn)
    val caseDetailsEntity = CaseDetailsEntity(crn, B3, COMMUNITY, "Jane", "Doe")

    assessRisksNeedsApi.riskSummaryResponse(crn)
    assessRisksNeedsApi.riskPredictorErrorResponse(crn)
    workforceAllocationsToDelius.getDeliusAllowedTeamsResponse(allocationDetails.allocatingStaff.code)
    workforceAllocationsToDelius.getDeliusAllowedTeamsResponse(allocationDetails.staff.code)
    caseDetailsRepository.save(caseDetailsEntity)
    val emailMessageResponse = notificationService.notifyAllocation(
      allocationDetails,
      allocateCase,
      caseDetailsEntity,
    )
    assertEquals("fc55e1ce-47d6-479c-ac80-3ac77c9fe609", emailMessageResponse.templateId)
  }
}
