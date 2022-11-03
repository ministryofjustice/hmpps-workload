package uk.gov.justice.digital.hmpps.hmppsworkload.integration.offenderManager

import com.microsoft.applicationinsights.TelemetryClient
import com.ninjasquad.springmockk.MockkBean
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.verify
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.hamcrest.core.IsNot
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.request.allocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses.emailResponse
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.RequirementManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WorkloadCalculationEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.service.AuditData
import uk.gov.justice.digital.hmpps.hmppsworkload.service.TelemetryEventType
import uk.gov.justice.digital.hmpps.hmppsworkload.service.TelemetryEventType.PERSON_MANAGER_ALLOCATED
import uk.gov.justice.digital.hmpps.hmppsworkload.service.getWmtPeriod
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import uk.gov.service.notify.SendEmailResponse
import java.math.BigInteger
import java.time.LocalDateTime

class AllocateCaseToOffenderManager : IntegrationTestBase() {

  @MockkBean
  private lateinit var notificationClient: NotificationClientApi
  @MockkBean
  private lateinit var telemetryClient: TelemetryClient

  private val crn = "CRN1"
  private val staffId = BigInteger.valueOf(123456L)

  private val staffCode = "OM1"
  private val teamCode = "T1"
  private val eventId = BigInteger.valueOf(123456789L)
  @BeforeEach
  fun setupApiCalls() {
    val allocatingOfficerUsername = "SOME_USER"
    singleActiveConvictionResponseForAllConvictions(crn)
    singleActiveConvictionResponse(crn)
    tierCalculationResponse(crn)
    singleActiveConvictionResponseForAllConvictions(crn)
    singleActiveInductionResponse(crn)
    staffUserNameResponse(allocatingOfficerUsername)
    riskSummaryErrorResponse(crn)
    riskPredictorResponse(crn)
    assessmentCommunityApiResponse(crn)
    caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.A0, CaseType.CUSTODY, "Jane", "Doe"))
    every { notificationClient.sendEmail(any(), any(), any(), any()) } returns
      SendEmailResponse(
        emailResponse()
      )

    every { telemetryClient.trackEvent(any(), any(), null) } returns Unit
    every { telemetryClient.context.operation.id } returns "fakeId"
  }

  @Test
  fun `can allocate CRN to Staff member`() {
    staffCodeResponse(staffCode, teamCode)
    offenderSummaryResponse(crn)
    singleActiveRequirementResponse(crn, eventId)

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.personManagerId")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
      .jsonPath("$.eventManagerId")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
      .jsonPath("$.requirementManagerIds[0]")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))

    expectWorkloadAllocationCompleteMessages(crn)

    await untilCallTo {
      workloadCalculationRepository.count()
    } matches { it == 1L }
    val actualWorkloadCalcEntity: WorkloadCalculationEntity =
      workloadCalculationRepository.findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)!!

    Assertions.assertAll(
      { Assertions.assertEquals(staffCode, actualWorkloadCalcEntity.staffCode) },
      { Assertions.assertEquals(teamCode, actualWorkloadCalcEntity.teamCode) },
      { Assertions.assertEquals(LocalDateTime.now().dayOfMonth, actualWorkloadCalcEntity.calculatedDate.dayOfMonth) },
      { Assertions.assertEquals(1, actualWorkloadCalcEntity.breakdownData.caseloadCount) }
    )

    verify(exactly = 2) { notificationClient.sendEmail(any(), any(), any(), any()) }
    verify(exactly = 1) {
      telemetryClient.trackEvent(
        PERSON_MANAGER_ALLOCATED.eventName,
        mapOf(
          "crn" to crn,
          "teamCode" to teamCode,
          "providerCode" to "N01",
          "staffId" to "123456",
          "wmtPeriod" to getWmtPeriod(LocalDateTime.now())
        ),
        null
      )
    }
  }

  @Test
  fun `Notify error still keeps entry in db`() {
    staffCodeResponse(staffCode, teamCode)
    offenderSummaryResponse(crn)
    val requirementId = BigInteger.valueOf(75315091L)
    singleActiveRequirementResponse(crn, eventId, requirementId)
    every { notificationClient.sendEmail(any(), any(), any(), any()) } throws NotificationClientException("An exception")
    caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.A0, CaseType.CUSTODY, "Jane", "Doe"))

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .is5xxServerError

    val personManager = personManagerRepository.findFirstByCrnOrderByCreatedDateDesc(crn)!!
    Assertions.assertEquals(staffCode, personManager.staffCode)
    Assertions.assertEquals(teamCode, personManager.teamCode)

    val eventManager = eventManagerRepository.findFirstByCrnAndEventIdOrderByCreatedDateDesc(crn, eventId)!!
    Assertions.assertEquals(staffCode, eventManager.staffCode)
    Assertions.assertEquals(teamCode, eventManager.teamCode)

    val requirementManager = requirementManagerRepository.findFirstByCrnAndEventIdAndRequirementIdOrderByCreatedDateDesc(crn, eventId, requirementId)!!
    Assertions.assertEquals(staffCode, requirementManager.staffCode)
    Assertions.assertEquals(teamCode, requirementManager.teamCode)
  }

  @Test
  fun `can allocate a requirement without a length`() {
    staffCodeResponse(staffCode, teamCode)
    offenderSummaryResponse(crn)
    singleActiveRequirementNoLengthResponse(crn, eventId)

    caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.A0, CaseType.CUSTODY, "Jane", "Doe"))

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.personManagerId")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
      .jsonPath("$.eventManagerId")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
      .jsonPath("$.requirementManagerIds[0]")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))

    expectWorkloadAllocationCompleteMessages(crn)
  }

  @Test
  fun `do not allocate active unpaid requirements`() {
    staffCodeResponse(staffCode, teamCode)
    offenderSummaryResponse(crn)
    singleActiveUnpaidRequirementResponse(crn, eventId)
    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.requirementManagerIds")
      .isEmpty
  }

  @Test
  fun `can allocate an already managed CRN to same staff member`() {
    val requirementId = BigInteger.valueOf(567891234L)
    staffCodeResponse(staffCode, teamCode)
    staffCodeResponse(staffCode, teamCode)
    offenderSummaryResponse(crn)
    singleActiveRequirementResponse(crn, eventId, requirementId)
    val storedPersonManager = PersonManagerEntity(crn = crn, staffId = staffId, staffCode = staffCode, teamCode = teamCode, offenderName = "John Doe", createdBy = "USER1", providerCode = "PV1", isActive = true)
    personManagerRepository.save(storedPersonManager)
    val storedEventManager = EventManagerEntity(crn = crn, staffId = staffId, staffCode = staffCode, teamCode = teamCode, eventId = eventId, createdBy = "USER1", providerCode = "PV1", isActive = true)
    eventManagerRepository.save(storedEventManager)
    val storedRequirementManager = RequirementManagerEntity(crn = crn, staffId = staffId, staffCode = staffCode, teamCode = teamCode, eventId = eventId, requirementId = requirementId, createdBy = "USER1", providerCode = "PV1", isActive = true)
    requirementManagerRepository.save(storedRequirementManager)

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.personManagerId")
      .isEqualTo(storedPersonManager.uuid.toString())
      .jsonPath("$.eventManagerId")
      .isEqualTo(storedEventManager.uuid.toString())
      .jsonPath("$.requirementManagerIds[0]")
      .isEqualTo(storedRequirementManager.uuid.toString())
  }

  @Test
  fun `can allocate an already managed CRN to different staff member`() {
    staffCodeResponse(staffCode, teamCode)
    offenderSummaryResponse(crn)
    singleActiveUnpaidRequirementResponse(crn, eventId)
    val otherPersonManager = PersonManagerEntity(crn = crn, staffId = BigInteger.ONE, staffCode = "ADIFFERENTCODE", teamCode = "TEAMCODE", offenderName = "John Doe", createdBy = "USER1", providerCode = "PV1", isActive = true)
    staffCodeResponse(otherPersonManager.staffCode, otherPersonManager.teamCode)
    val storedPersonManager = personManagerRepository.save(otherPersonManager)
    val storedEventManager = eventManagerRepository.save(EventManagerEntity(crn = crn, eventId = eventId, staffId = BigInteger.ONE, staffCode = "ADIFFERENTCODE", teamCode = "TEAMCODE", createdBy = "USER1", providerCode = "PV1", isActive = true))

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.personManagerId")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
      .jsonPath("$.personManagerId")
      .value(IsNot.not(storedPersonManager.uuid.toString()))

    await untilCallTo {
      workloadCalculationRepository.count()
    } matches { it == 2L }

    val actualWorkloadCalcEntity: WorkloadCalculationEntity? =
      workloadCalculationRepository.findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(storedPersonManager.staffCode, storedPersonManager.teamCode)

    Assertions.assertAll(
      { Assertions.assertEquals(storedPersonManager.staffCode, actualWorkloadCalcEntity!!.staffCode) },
      { Assertions.assertEquals(storedPersonManager.teamCode, actualWorkloadCalcEntity!!.teamCode) },
      { Assertions.assertEquals(LocalDateTime.now().dayOfMonth, actualWorkloadCalcEntity!!.calculatedDate.dayOfMonth) }
    )

    val previousPersonManager = personManagerRepository.findByIdOrNull(storedPersonManager.id!!)!!
    Assertions.assertFalse(previousPersonManager.isActive)

    val previousEventManager = eventManagerRepository.findByIdOrNull(storedEventManager.id!!)!!
    Assertions.assertFalse(previousEventManager.isActive)
  }

  @Test
  fun `only send the email once when clicking allocate multiple times`() {
    staffCodeResponse(staffCode, teamCode)
    staffCodeResponse(staffCode, teamCode)
    offenderSummaryResponse(crn)
    offenderSummaryResponse(crn)
    singleActiveRequirementResponse(crn, eventId)
    singleActiveRequirementResponse(crn, eventId)

    caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.A0, CaseType.CUSTODY, "Jane", "Doe"))

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    verify(exactly = 1) { notificationClient.sendEmail(any(), "sheila.hancock@test.justice.gov.uk", any(), any()) }
    verify(exactly = 1) { notificationClient.sendEmail(any(), "additionalEmailReciever@test.justice.gov.uk", any(), any()) }

    clearAllMocks()

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    verify(exactly = 0) { notificationClient.sendEmail(any(), "sheila.hancock@test.justice.gov.uk", any(), any()) }
    verify(exactly = 0) { notificationClient.sendEmail(any(), "additionalEmailReciever@test.justice.gov.uk", any(), any()) }
  }

  @Test
  fun `must emit staff grade to tier allocation telemetry event`() {
    staffCodeResponse(staffCode, teamCode)
    offenderSummaryResponse(crn)
    singleActiveRequirementResponse(crn, eventId)

    val caseDetailsEntity = CaseDetailsEntity(crn, Tier.A0, CaseType.CUSTODY, "Jane", "Doe")
    caseDetailsRepository.save(caseDetailsEntity)
    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    verify(exactly = 1) {
      telemetryClient.trackEvent(
        TelemetryEventType.STAFF_GRADE_TIER_ALLOCATED.eventName,
        mapOf(
          "teamCode" to teamCode,
          "providerCode" to "N01",
          "staffGrade" to "PO",
          "tier" to caseDetailsEntity.tier.name,
        ),
        null
      )
    }
  }

  @Test
  fun `must emit staff grade to tier allocation telemetry event without case details`() {
    staffCodeResponse(staffCode, teamCode)
    offenderSummaryResponse(crn)
    singleActiveRequirementResponse(crn, eventId)

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    verify(exactly = 1) {
      telemetryClient.trackEvent(
        TelemetryEventType.STAFF_GRADE_TIER_ALLOCATED.eventName,
        mapOf(
          "teamCode" to teamCode,
          "providerCode" to "N01",
          "staffGrade" to "PO",
          "tier" to "A0",
        ),
        null
      )
    }
  }

  @Test
  fun `can send audit data when allocating`() {
    staffCodeResponse(staffCode, teamCode)
    offenderSummaryResponse(crn)
    singleActiveRequirementResponse(crn, eventId)

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    await untilCallTo { verifyAuditMessageOnQueue() } matches { it == true }
  }

  @Test
  fun `audit data contain required fields`() {
    staffCodeResponse(staffCode, teamCode)
    offenderSummaryResponse(crn)
    val requirementId = BigInteger.valueOf(75315091L)
    singleActiveRequirementResponse(crn, eventId, requirementId)

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk

    await untilCallTo { verifyAuditMessageOnQueue() } matches { it == true }
    val auditData = AuditData(crn, eventId, listOf(requirementId))
    Assertions.assertEquals(objectMapper.writeValueAsString(auditData), getAuditMessages().details)
  }

  @Test
  fun `can send email when selecting a second person to receive email`() {
    staffCodeResponse(staffCode, teamCode)
    offenderSummaryResponse(crn)
    singleActiveRequirementResponse(crn, eventId)

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.personManagerId")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
      .jsonPath("$.eventManagerId")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
      .jsonPath("$.requirementManagerIds[0]")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))

    expectWorkloadAllocationCompleteMessages(crn)

    await untilCallTo {
      workloadCalculationRepository.count()
    } matches { it == 1L }
    val actualWorkloadCalcEntity: WorkloadCalculationEntity =
      workloadCalculationRepository.findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)!!

    Assertions.assertAll(
      { Assertions.assertEquals(staffCode, actualWorkloadCalcEntity.staffCode) },
      { Assertions.assertEquals(teamCode, actualWorkloadCalcEntity.teamCode) },
      { Assertions.assertEquals(LocalDateTime.now().dayOfMonth, actualWorkloadCalcEntity.calculatedDate.dayOfMonth) },
      { Assertions.assertEquals(1, actualWorkloadCalcEntity.breakdownData.caseloadCount) }
    )
    // verify that the additional email got an email
    verify(exactly = 1) { notificationClient.sendEmail(any(), "additionalEmailReciever@test.justice.gov.uk", any(), any()) }
    // verify that the allocated-to officer got an email
    verify(exactly = 1) { notificationClient.sendEmail(any(), "sheila.hancock@test.justice.gov.uk", any(), any()) }
    verify(exactly = 1) {
      telemetryClient.trackEvent(
        PERSON_MANAGER_ALLOCATED.eventName,
        mapOf(
          "crn" to crn,
          "teamCode" to teamCode,
          "providerCode" to "N01",
          "staffId" to "123456",
          "wmtPeriod" to getWmtPeriod(LocalDateTime.now())
        ),
        null
      )
    }
  }

  @Test
  fun `can send email by default when checkbox unchecked`() {
    val allocateToEmail = "allocateTo-user@test.justice.gov.uk"
    staffCodeResponse(staffCode, teamCode, email = allocateToEmail)
    offenderSummaryResponse(crn)
    singleActiveRequirementResponse(crn, eventId)

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.personManagerId")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
      .jsonPath("$.eventManagerId")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
      .jsonPath("$.requirementManagerIds[0]")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))

    expectWorkloadAllocationCompleteMessages(crn)

    await untilCallTo {
      workloadCalculationRepository.count()
    } matches { it == 1L }

    // verify that the additional email received an email
    verify(exactly = 1) { notificationClient.sendEmail(any(), "additionalEmailReciever@test.justice.gov.uk", any(), any()) }
    // verify that the allocating officer received an email
    verify(exactly = 1) { notificationClient.sendEmail(any(), "sheila.hancock@test.justice.gov.uk", any(), any()) }
    // verify that the allocate-to user received an email.
    verify(exactly = 1) { notificationClient.sendEmail(any(), allocateToEmail, any(), any()) }
  }
  @Test
  fun `do not send email when checkbox checked`() {
    val allocateToEmail = "allocateTo-user@test.justice.gov.uk"
    staffCodeResponse(staffCode, teamCode, email = allocateToEmail)
    offenderSummaryResponse(crn)
    singleActiveRequirementResponse(crn, eventId)

    webTestClient.post()
      .uri("/team/$teamCode/offenderManager/$staffCode/case")
      .bodyValue(allocateCase(crn, eventId, false))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.personManagerId")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
      .jsonPath("$.eventManagerId")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))
      .jsonPath("$.requirementManagerIds[0]")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))

    expectWorkloadAllocationCompleteMessages(crn)

    await untilCallTo {
      workloadCalculationRepository.count()
    } matches { it == 1L }

    // verify that the additional email received an email
    verify(exactly = 1) { notificationClient.sendEmail(any(), "additionalEmailReciever@test.justice.gov.uk", any(), any()) }
    // verify that the allocate-to user received an email.
    verify(exactly = 1) { notificationClient.sendEmail(any(), allocateToEmail, any(), any()) }
    // verify that the allocating officer does not receive an email
    verify(exactly = 0) { notificationClient.sendEmail(any(), "sheila.hancock@test.justice.gov.uk", any(), any()) }
  }
}
