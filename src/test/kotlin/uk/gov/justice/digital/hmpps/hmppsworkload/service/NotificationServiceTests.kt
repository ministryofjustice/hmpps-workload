package uk.gov.justice.digital.hmpps.hmppsworkload.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllReoffendingPredictor
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.CombinedSeriousReoffendingPredictor
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.DeliusTeams
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.GroupReconvictionScore
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Manager
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Name
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.OffenceDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ReallocationDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Requirement
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskOfSeriousRecidivismScore
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskPredictorOutputV1
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskPredictorOutputV2
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskPredictorV1
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskPredictorV2
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskSummary
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.SentenceDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.ReallocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.getAllocationDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.utils.DateUtils
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Collections.emptySet

class NotificationServiceTests {
  private val notificationClient = mockk<NotificationClientApi>()
  private val sqsSuccessPublisher = mockk<SqsSuccessPublisher>()
  private val assessRisksNeedsApiClient = mockk<AssessRisksNeedsApiClient>()
  private val templateId = "templateId"
  private val laoTemplateID = "laoTemplateId"
  private val reallocationTemplateId = "reallocationTemplateId"
  private val reallocationTemplateLAOId = "reallocationLaoTemplateId"
  private val reallocationPreviousTemplateId = "reallocationPreviousTemplateId"
  private val reallocationPreviousTemplateLAOId = "reallocationPreviousTemplateLaoId"
  private val workforceAllocationsToDeliusApiClient = mockk<WorkforceAllocationsToDeliusApiClient>()
  private val meterRegistry = mockk<MeterRegistry>()
  private val counter = mockk<Counter>()
  private val notificationService = NotificationService(
    notificationClient,
    templateId,
    laoTemplateID,
    reallocationTemplateId,
    reallocationTemplateLAOId,
    reallocationPreviousTemplateId,
    reallocationPreviousTemplateLAOId,
    assessRisksNeedsApiClient,
    sqsSuccessPublisher,
    workforceAllocationsToDeliusApiClient,
    meterRegistry,
  )
  private val allocateCase = AllocateCase(
    "CRN1111",
    sendEmailCopyToAllocatingOfficer = false,
    eventNumber = 1,
    allocationJustificationNotes = "Some Notes",
    sensitiveNotes = false,
    spoOversightNotes = "spo notes",
    sensitiveOversightNotes = null,
    laoCase = false,
  )
  private val parameters = mapOf(
    "officer_name" to "Staff Member",
    "induction_statement" to "no initial appointment needed",
    "requirements" to emptyList<Requirement>(),
    "rosh" to "Score Unavailable",
    "rsrLevel" to "Score Unavailable",
    "rsrPercentage" to "N/A",
    "ogrsLevel" to "Score Unavailable",
    "ogrsPercentage" to "N/A",
    "court_name" to "Court Name",
    "sentence_date" to "7 October 2022",
    "offences" to emptyList<OffenceDetails>(),
    "order" to "CUSTODY (6 Months)",
    "case_name" to "Jonathon Jones",
    "crn" to "CRN1111",
    "notes" to "Some Notes",
    "allocatingOfficerName" to "Allocating Member",
    "allocatingOfficerGrade" to "SPO",
  )

  private val caseDetails = CaseDetailsEntity("", Tier.B3, CaseType.CUSTODY, "Jane", "Doe")

  @BeforeEach
  fun setup() {
    coEvery { assessRisksNeedsApiClient.getRiskSummary(any()) } returns null
    coEvery { assessRisksNeedsApiClient.getRiskPredictors(any()) } returns emptyList()
    coEvery { sqsSuccessPublisher.sendNotification(any()) } returns Unit
    coEvery { workforceAllocationsToDeliusApiClient.getDeliusAllowedTeamInfo(any()) } returns DeliusTeams(emptyList(), emptyList())
  }

  @Test
  fun `must add case name to email`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals(allocationDetails.name.getCombinedName(), parameters.captured.emailParameters["case_name"])
  }

  @Test
  fun `must add crn to email`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) {
      sqsSuccessPublisher.sendNotification(capture(parameters))
    }
    Assertions.assertEquals(allocateCase.crn, parameters.captured.emailParameters["crn"])
  }

  @Test
  fun `must add officer name`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals(allocationDetails.staff.name.getCombinedName(), parameters.captured.emailParameters["officer_name"])
  }

  @Test
  fun `must add court name`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals(allocationDetails.court.name, parameters.captured.emailParameters["court_name"])
  }

  @Test
  fun `must add court sentence date`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals(
      allocationDetails.sentence.date.withZoneSameInstant(ZoneId.systemDefault()).format(DateUtils.notifyDateFormat),
      parameters.captured.emailParameters["sentence_date"],
    )
  }

  @Test
  fun `must add induction statement no induction appointment needed when case type is custody`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals("no initial appointment needed", parameters.captured.emailParameters["induction_statement"])
  }

  @Test
  fun `must add induction statement booked and due on when initial appointment is booked in the future`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn, LocalDate.now().plusDays(5L))
    val caseDetails = CaseDetailsEntity("", Tier.B3, CaseType.COMMUNITY, "Jane", "Doe")

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals("their initial appointment is scheduled for ${allocationDetails.initialAppointment!!.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))}", parameters.captured.emailParameters["induction_statement"])
  }

  @Test
  fun `must add induction statement is overdue and was due on when initial appointment is booked in the past`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn, LocalDate.now().minusDays(5L))
    val caseDetails = CaseDetailsEntity("", Tier.B3, CaseType.COMMUNITY, "Jane", "Doe")

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals("their initial appointment was scheduled for ${allocationDetails.initialAppointment!!.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))}", parameters.captured.emailParameters["induction_statement"])
  }

  @Test
  fun `must add induction statement has not been booked and is due on when initial appointment is not booked at all`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    val caseDetails = CaseDetailsEntity("", Tier.B3, CaseType.COMMUNITY, "Jane", "Doe")

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals("no date found for the initial appointment, please check with your team", parameters.captured.emailParameters["induction_statement"])
  }

  @Test
  fun `must add offences`() = runBlocking {
    val offenceDetails = OffenceDetails("Offence main category")
    val allocationDetails = getAllocationDetails(allocateCase.crn, offenceDetails = listOf(offenceDetails))

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals(listOf(offenceDetails.mainCategory), parameters.captured.emailParameters["offences"])
  }

  @Test
  fun `must add order`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals("${allocationDetails.sentence.description} (${allocationDetails.sentence.length})", parameters.captured.emailParameters["order"])
  }

  @Test
  fun `must add requirements`() = runBlocking {
    val name = Name(
      "fed",
      "E",
      "flintstomne",
    )
    val manager = Manager(
      "fred",
      "liverpool",
      "top",
      name,
      true,
    )
    val requirement = Requirement(
      "Main Category",
      "Sub Category",
      "1 Year",
      BigInteger.ONE,
      manager,
      true,
    )
    val allocationDetails = getAllocationDetails(allocateCase.crn, activeRequirements = listOf(requirement))

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals(listOf("${requirement.mainCategory}: ${requirement.subCategory} ${requirement.length}"), parameters.captured.emailParameters["requirements"])
  }

  @Test
  fun `must add requirements without length`() = runBlocking {
    val name = Name(
      "fed",
      "E",
      "flintstomne",
    )
    val manager = Manager(
      "fred",
      "liverpool",
      "top",
      name,
      true,
    )
    val requirement = Requirement(
      "Main Category",
      "Sub Category",
      "",
      BigInteger.ONE,
      manager,
      true,
    )
    val allocationDetails = getAllocationDetails(allocateCase.crn, activeRequirements = listOf(requirement))

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals(listOf("${requirement.mainCategory}: ${requirement.subCategory}"), parameters.captured.emailParameters["requirements"])
  }

  @Test
  fun `must add requirement with null subcategory`() = runBlocking {
    val name = Name(
      "fed",
      "E",
      "flintstomne",
    )
    val manager = Manager(
      "fred",
      "liverpool",
      "top",
      name,
      true,
    )
    val requirement = Requirement(
      "Main Category",
      null,
      "",
      BigInteger.ONE,
      manager,
      true,
    )
    val allocationDetails = getAllocationDetails(allocateCase.crn, activeRequirements = listOf(requirement))

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals(listOf("${requirement.mainCategory}: ${requirement.mainCategory}"), parameters.captured.emailParameters["requirements"])
  }

  @Test
  fun `must add rosh capitalized when it exists`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    coEvery { assessRisksNeedsApiClient.getRiskSummary(allocateCase.crn) } returns RiskSummary("HIGH")

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals("High", parameters.captured.emailParameters["rosh"])
  }

  @Test
  fun `must add RSR and OGRS details correctly capitalized when they exists with V1 Risk Predictor Response`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    val riskPredictor = RiskPredictorV1(
      LocalDateTime.now(),
      null,
      null,
      "1",
      RiskPredictorOutputV1(
        GroupReconvictionScore(null, BigDecimal.TEN, "LOW"),
        null,
        null,
        RiskOfSeriousRecidivismScore(BigDecimal.TEN, null, null, null, "MEDIUM"),
        null,
      ),
    )
    coEvery { assessRisksNeedsApiClient.getRiskPredictors(allocateCase.crn) } returns listOf(riskPredictor)

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals("Medium", parameters.captured.emailParameters["rsrLevel"])
    Assertions.assertEquals(riskPredictor.getRSRPercentageScore().toString(), parameters.captured.emailParameters["rsrPercentage"])
    Assertions.assertEquals("Low", parameters.captured.emailParameters["ogrsLevel"])
    Assertions.assertEquals(riskPredictor.getOGRSPercentageScore().toString(), parameters.captured.emailParameters["ogrsPercentage"])
  }

  @Test
  fun `must add RSR and OGRS details correctly capitalized when they exists with V2 Risk Predictor Response`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    val riskPredictor = RiskPredictorV2(
      LocalDateTime.now(),
      null,
      null,
      "1",
      RiskPredictorOutputV2(
        AllReoffendingPredictor(null, BigDecimal.TEN, "LOW"),
        null,
        null,
        null,
        null,
        CombinedSeriousReoffendingPredictor(null, null, BigDecimal.TEN, "MEDIUM"),
      ),
    )
    coEvery { assessRisksNeedsApiClient.getRiskPredictors(allocateCase.crn) } returns listOf(riskPredictor)

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals("Medium", parameters.captured.emailParameters["rsrLevel"])
    Assertions.assertEquals(riskPredictor.getRSRPercentageScore().toString(), parameters.captured.emailParameters["rsrPercentage"])
    Assertions.assertEquals("Low", parameters.captured.emailParameters["ogrsLevel"])
    Assertions.assertEquals(riskPredictor.getOGRSPercentageScore().toString(), parameters.captured.emailParameters["ogrsPercentage"])
  }

  @Test
  fun `must add notes when they exist`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    val allocateCase = AllocateCase("CRN1111", "Some Notes", sendEmailCopyToAllocatingOfficer = false, eventNumber = 1, allocationJustificationNotes = "Some Notes", sensitiveNotes = false, spoOversightNotes = "spo notes", sensitiveOversightNotes = null, laoCase = false)

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals(allocateCase.instructions, parameters.captured.emailParameters["notes"])
  }

  @Test
  fun `must add allocating officer name`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    val allocateCase = AllocateCase("CRN1111", "Some Notes", sendEmailCopyToAllocatingOfficer = false, eventNumber = 1, allocationJustificationNotes = "some notes", sensitiveNotes = false, spoOversightNotes = "spo notes", sensitiveOversightNotes = null, laoCase = false)

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals(allocationDetails.allocatingStaff.name.getCombinedName(), parameters.captured.emailParameters["allocatingOfficerName"])
  }

  @Test
  fun `must add allocating officer grade`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)

    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    Assertions.assertEquals(allocationDetails.allocatingStaff.getGrade(), parameters.captured.emailParameters["allocatingOfficerGrade"])
  }

  @Test
  fun `must email all addresses supplied`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    val firstEmail = "first@email.com"
    val secondEmail = "second@email.com"
    val allocateCase = AllocateCase("CRN1111", "instructions", listOf(firstEmail, secondEmail), false, 1, allocationJustificationNotes = "some notes", sensitiveNotes = false, spoOversightNotes = "spo notes", sensitiveOversightNotes = null, laoCase = false)
    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
  }

  @Test
  fun `must send email by default to allocating officer`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    val firstEmail = "first@justice.gov.uk"
    val secondEmail = "second@justice.gov.uk"
    val allocateCase = AllocateCase("CRN1111", "instructions", listOf(firstEmail, secondEmail), true, 1, allocationJustificationNotes = "some notes", sensitiveNotes = false, spoOversightNotes = "spo notes", sensitiveOversightNotes = null, laoCase = false)
    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    val emailTo = HashSet<String>(parameters.captured.emailTo ?: emptySet())

    Assertions.assertEquals(emailTo.size, 4)
    Assertions.assertTrue(emailTo.contains(allocationDetails.allocatingStaff.email!!))
  }

  @Test
  fun `must send reallocation email to new  officer and not allocating officer or previous officer`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    val firstEmail = "first@justice.gov.uk"
    val secondEmail = "second@justice.gov.uk"
    val allocateCase = ReallocateCase(
      "CRN1111", listOf(firstEmail, secondEmail), false, true, "spo notes",
      laoCase = false, allocationReason = null, nextAppointmentDate = null, lastOasysAssessmentDate = null, failureToComply = null,
    )
    val reallocationDetails = ReallocationDetails("Laziness", "never", "tomorrow", "12", getManager(), ArrayList<Requirement>(), ArrayList<OffenceDetails>(), ArrayList<SentenceDetails>())
    notificationService.notifyReallocation(allocationDetails, allocateCase, Tier.A1.name, reallocationDetails)

    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    val emailTo = HashSet<String>(parameters.captured.emailTo ?: emptySet())

    Assertions.assertEquals(emailTo.size, 3)
    Assertions.assertFalse(emailTo.contains("reallocatedFrom@notifications.service.gov.uk"))
    Assertions.assertTrue(emailTo.contains("simulate-delivered@notifications.service.gov.uk"))
    Assertions.assertFalse(emailTo.contains("simulate-delivered@justice.gov.uk"))
  }

  @Test
  fun `must send reallocation email to new  officer and email previous officer`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    val firstEmail = "first@justice.gov.uk"
    val secondEmail = "second@justice.gov.uk"
    val allocateCase = ReallocateCase(
      "CRN1111", listOf(firstEmail, secondEmail), true, true, "spo notes",
      laoCase = false, allocationReason = null, nextAppointmentDate = null, lastOasysAssessmentDate = null, failureToComply = null,
    )
    val reallocationDetails = ReallocationDetails("Laziness", "never", "tomorrow", "12", getManager(), ArrayList<Requirement>(), ArrayList<OffenceDetails>(), ArrayList<SentenceDetails>())
    notificationService.notifyReallocation(allocationDetails, allocateCase, Tier.A1.name, reallocationDetails)

    var parameters = mutableListOf<NotificationEmail>()

    coVerify(exactly = 2) { sqsSuccessPublisher.sendNotification(capture(parameters)) }

    val firstNotification = parameters[0]
    val secondNotification = parameters[1]

    val emailTo = HashSet<String>(firstNotification.emailTo)
    val emailToPrevious = HashSet<String>(secondNotification.emailTo)

    Assertions.assertEquals(emailTo.size, 3)
    Assertions.assertTrue(emailTo.contains("simulate-delivered@notifications.service.gov.uk"))
    Assertions.assertFalse(emailTo.contains("simulate-delivered@justice.gov.uk"))

    Assertions.assertEquals(emailToPrevious.size, 1)
    Assertions.assertTrue(emailToPrevious.contains("reallocatedFrom@notifications.service.gov.uk"))
  }

  fun getManager() = StaffMember(
    "STAFF1",
    Name(
      "Staff",
      "",
      "Member",
    ),
    "reallocatedFrom@notifications.service.gov.uk",
    "PO",
  )

  @Test
  fun `must not send email to allocating officer if opted out `() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    val firstEmail = "first@justice.gov.uk"
    val secondEmail = "second@justice.gov.uk"
    val allocateCase = AllocateCase(
      "CRN1111", "instructions", listOf(firstEmail, secondEmail), false, 1, allocationJustificationNotes = "some notes", sensitiveNotes = false, spoOversightNotes = "spo notes", sensitiveOversightNotes = null,
      laoCase = false,
    )
    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    val parameters = slot<NotificationEmail>()
    coVerify(exactly = 1) { sqsSuccessPublisher.sendNotification(capture(parameters)) }
    val emailTo = HashSet<String>(parameters.captured.emailTo ?: emptySet())

    Assertions.assertEquals(emailTo.size, 3)
    Assertions.assertFalse(emailTo.contains(allocationDetails.allocatingStaff.email!!))
  }

  @Test
  fun `Calls meter registry if email send fails`() = runBlocking {
    val allocationDetails = getAllocationDetails(allocateCase.crn)
    val firstEmail = "first@justice.gov.uk"
    val secondEmail = "second@justice.gov.ww"
    val allocateCase = AllocateCase(
      "CRN1111", "instructions", listOf(firstEmail, secondEmail), false, 1, allocationJustificationNotes = "some notes", sensitiveNotes = false, spoOversightNotes = "spo notes", sensitiveOversightNotes = null,
      laoCase = false,
    )
    coEvery { sqsSuccessPublisher.sendNotification(any()) } throws NotificationClientException("Failed to send email")
    coEvery { meterRegistry.counter(any(), any(), any()) } returns counter
    coEvery { counter.increment() } returns Unit
    notificationService.notifyAllocation(allocationDetails, allocateCase, caseDetails)
    coVerify { meterRegistry.counter(any(), any(), any()) }
  }
}
