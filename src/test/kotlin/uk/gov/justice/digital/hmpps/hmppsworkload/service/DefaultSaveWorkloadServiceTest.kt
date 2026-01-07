package uk.gov.justice.digital.hmpps.hmppsworkload.service

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocatedActiveEvent
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocatedCaseView
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocatedEventRequirement
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocationDemandDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Court
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.CrnDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.InitialAppointment
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Manager
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Name
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.OffenceDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.OfficerView
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ReallocationDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Requirement
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskOGRS
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.SentenceDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.ReallocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.SaveResult
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.RequirementManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.SaveEventManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.SavePersonManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.SaveRequirementManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.utils.capitalize
import java.math.BigInteger
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

private const val OFFICER_EMAIL = "me@here.com"
private const val OFFICER_GRADE = "SPO"
private const val OFFICER_CODE = "007"
private const val STAFF_CODE = "001"
private const val PREVIOUS_STAFF_CODE = "002"
private const val STAFF_TEAM_CODE = "Reds"

class DefaultSaveWorkloadServiceTest {
  private val workforceAllocationsToDeliusApiClient = mockk<WorkforceAllocationsToDeliusApiClient>()
  private val saveEventManagerService = mockk<SaveEventManagerService>()
  private val savePersonManagerService = mockk<SavePersonManagerService>()
  private val saveRequirementManagerService = mockk<SaveRequirementManagerService>()
  private val notificationService = mockk<NotificationService>()
  private val telemetryService = mockk<TelemetryService>()
  private val caseDetailsRepository = mockk<CaseDetailsRepository>()
  private val sqsSuccessPublisher = mockk<SqsSuccessPublisher>()

  private val defaultSaveWorkloadService = DefaultSaveWorkloadService(
    savePersonManagerService,
    workforceAllocationsToDeliusApiClient,
    saveEventManagerService,
    saveRequirementManagerService,
    notificationService,
    telemetryService,
    sqsSuccessPublisher,
    caseDetailsRepository,
  )

  @Test
  fun `saves workload details and make correct calls to notify and sqs`() {
    val uuid = UUID.fromString("02df6973-fdce-4f4a-9c79-7f7b6f73e48e")
    runBlocking {
      val crn = "1234"
      val loggedInUser = "me"
      val allocateCase = AllocateCase(
        crn, instructions = "", emailTo = null, sendEmailCopyToAllocatingOfficer = false, eventNumber = 1, allocationJustificationNotes = "Some Notes", sensitiveNotes = false, spoOversightNotes = "spo notes", sensitiveOversightNotes = null,
        laoCase = false,
      )

      val eventNumber = 1
      val name = Name("Jim", "A", "Bond")
      val staffIdentifier = StaffIdentifier(STAFF_CODE, STAFF_TEAM_CODE)
      val staffMember = StaffMember(OFFICER_CODE, name, OFFICER_EMAIL, OFFICER_GRADE)
      val allocatingStaffMember = StaffMember(STAFF_CODE, name, OFFICER_EMAIL, OFFICER_GRADE)
      val caseDetails = CaseDetailsEntity(crn, Tier.A1, CaseType.CUSTODY, "John", "Smith")

      coEvery { caseDetailsRepository.findByIdOrNull(crn) } returns caseDetails
      val sentence = SentenceDetails("fraud", ZonedDateTime.now(), "life")
      val court = Court("Nottm", LocalDate.now())
      val id = BigInteger.valueOf(11L)
      val manager = Manager("003", "001", "SPO", name, false)
      val requirements = listOf(Requirement("Cat 1", "Cat 2", "4 days", id, manager, true))
      val offences = listOf(OffenceDetails("one"))
      val allocationDemandDetails = AllocationDemandDetails(
        crn, name, staffMember, allocatingStaffMember,
        InitialAppointment(date = LocalDate.now()),
        RiskOGRS(LocalDate.now(), 10),
        sentence, court, offences, requirements,
      )
      coEvery { workforceAllocationsToDeliusApiClient.allocationDetails(crn, eventNumber, STAFF_CODE, loggedInUser) } returns
        allocationDemandDetails

      val eventManagerEntity = SaveResult(EventManagerEntity(null, UUID.randomUUID(), crn, "", "", "", ZonedDateTime.now(), true, 1, null, "Bond"), true)
      coEvery { saveEventManagerService.saveEventManager("Reds", any(), any(), loggedInUser, STAFF_CODE, name.getCombinedName()) } returns
        eventManagerEntity

      val personManagerEntity = PersonManagerEntity(1L, uuid, crn, STAFF_CODE, STAFF_TEAM_CODE, loggedInUser, ZonedDateTime.now(), true, allocationReason = AllocationReason.INITIAL_ALLOCATION)
      coEvery { savePersonManagerService.savePersonManager("Reds", staffMember, loggedInUser, crn, allocationReason = AllocationReason.INITIAL_ALLOCATION) } returns
        SaveResult(personManagerEntity, true)

      val requirementManagerEntity = RequirementManagerEntity(1L, uuid, crn, BigInteger.valueOf(1L), STAFF_CODE, STAFF_TEAM_CODE, "someone", ZonedDateTime.now(), true, 1, allocationReason = AllocationReason.INITIAL_ALLOCATION)
      coEvery { saveRequirementManagerService.saveRequirementManagers(STAFF_TEAM_CODE, staffMember, allocateCase.crn, allocateCase.eventNumber, AllocationReason.INITIAL_ALLOCATION, loggedInUser, any()) } returns
        listOf(
          SaveResult(requirementManagerEntity, true),
        )

      coEvery { telemetryService.trackRequirementManagerAllocated(requirementManagerEntity, caseDetails) } just Runs
      coEvery { telemetryService.trackPersonManagerAllocated(personManagerEntity, caseDetails) } just Runs
      coEvery { telemetryService.trackEventManagerAllocated(eventManagerEntity.entity, caseDetails) } just Runs
      coEvery { telemetryService.trackEventManagerAllocated(eventManagerEntity.entity, caseDetails) } just Runs
      coEvery { telemetryService.trackStaffGradeToTierAllocated(caseDetails, staffMember, STAFF_TEAM_CODE) } just Runs

      coEvery { sqsSuccessPublisher.updatePerson(crn, any(), any()) } just Runs
      coEvery { sqsSuccessPublisher.updateEvent(crn, any(), any()) } just Runs
      coEvery { sqsSuccessPublisher.updateRequirement(crn, any(), any()) } just Runs
      coEvery { sqsSuccessPublisher.auditAllocation(crn, any(), any(), any()) } just Runs

      coEvery { notificationService.notifyAllocation(allocationDemandDetails, allocateCase, caseDetails) } returns
        NotificationMessageResponse("template", "ref1", setOf("me@there.com"))

      val workload = defaultSaveWorkloadService.saveWorkload(staffIdentifier, allocateCase, loggedInUser)

      assertEquals(workload!!.eventManagerId, eventManagerEntity.entity.uuid)
      assertEquals(workload.requirementManagerIds, listOf(requirementManagerEntity.uuid))
      assertEquals(workload.personManagerId, personManagerEntity.uuid)

      coVerify(exactly = 1) { notificationService.notifyAllocation(allocationDemandDetails, allocateCase, caseDetails) }

      coVerify(exactly = 1) { sqsSuccessPublisher.updatePerson(crn, any(), any()) }
      coVerify(exactly = 1) { sqsSuccessPublisher.updateEvent(crn, any(), any()) }
      coVerify(exactly = 1) { sqsSuccessPublisher.updateRequirement(crn, any(), any()) }
      coVerify(exactly = 1) { sqsSuccessPublisher.auditAllocation(crn, any(), any(), any()) }

      coVerify(exactly = 1) { telemetryService.trackStaffGradeToTierAllocated(caseDetails, staffMember, STAFF_TEAM_CODE) }
      coVerify(exactly = 1) { telemetryService.trackPersonManagerAllocated(personManagerEntity, caseDetails) }
      coVerify(exactly = 1) { telemetryService.trackEventManagerAllocated(eventManagerEntity.entity, caseDetails) }
      coVerify(exactly = 1) { telemetryService.trackRequirementManagerAllocated(requirementManagerEntity, caseDetails) }
    }
  }

  @Test
  fun `saves workload reallocation details and make correct calls to notify and sqs`() {
    val uuid = UUID.fromString("02df6973-fdce-4f4a-9c79-7f7b6f73e48e")
    runBlocking {
      val crn = "1234"
      val loggedInUser = "me"
      val allocateCase = ReallocateCase(
        crn, emailTo = null, sensitiveNotes = false, reallocationNotes = "spo notes",
        laoCase = false, allocationReason = AllocationReason.RISK_TO_STAFF, nextAppointmentDate = null, lastOasysAssessmentDate = null, failureToComply = null,
      )

      val name = Name("Jim", "A", "Bond")

      val staffIdentifier = StaffIdentifier(STAFF_CODE, STAFF_TEAM_CODE)

      val staffMember = StaffMember(OFFICER_CODE, name, OFFICER_EMAIL, OFFICER_GRADE)
      val allocatingStaffMember = StaffMember(STAFF_CODE, name, OFFICER_EMAIL, OFFICER_GRADE)
      val caseDetails = CaseDetailsEntity(crn, Tier.A1, CaseType.CUSTODY, "John", "Smith")

      coEvery { caseDetailsRepository.findByIdOrNull(crn) } returns caseDetails
      val sentence = SentenceDetails("fraud", ZonedDateTime.now(), "life")
      val court = Court("Nottm", LocalDate.now())
      val id = BigInteger.valueOf(11L)
      val manager = Manager(PREVIOUS_STAFF_CODE, "001", "SPO", name, false)
      val crnDetails = CrnDetails(crn, name, LocalDate.now(), manager, true)

      val requirements = listOf(Requirement("Cat 1", "Cat 2", "4 days", id, manager, true))
      val offences = listOf(OffenceDetails("one"))
      val allocationDemandDetails = AllocationDemandDetails(
        crn, name, staffMember, allocatingStaffMember,
        InitialAppointment(date = LocalDate.now()),
        RiskOGRS(LocalDate.now(), 10),
        sentence, court, offences, requirements,
      )

      val activeRequirements = listOf(AllocatedEventRequirement("Cat 1", "Cat 2", "9 miles"), AllocatedEventRequirement("Cat 2", "Cat 4", "7 days"))
      val activeRequirements2 = listOf(AllocatedEventRequirement("Cat A", "Cat B", "4 years"), AllocatedEventRequirement("Cat 2", "Cat 4", "7 brides"))

      val activeEvents = listOf(
        AllocatedActiveEvent(3, 4, null, null, emptyList(), activeRequirements),
        AllocatedActiveEvent(1, 2, null, null, emptyList(), activeRequirements2),
        AllocatedActiveEvent(2, 2, null, null, emptyList(), activeRequirements2),
      )

      val allocatedCaseView = AllocatedCaseView(name, LocalDate.now(), "Male", "pnc", null, null, activeEvents)

      coEvery { workforceAllocationsToDeliusApiClient.getCrnDetails(crn) } returns crnDetails
      coEvery { workforceAllocationsToDeliusApiClient.getOfficerView(PREVIOUS_STAFF_CODE) } returns OfficerView(PREVIOUS_STAFF_CODE, name, "SPO", null, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE)
      coEvery { workforceAllocationsToDeliusApiClient.allocationDetails(crn, 1, STAFF_CODE, loggedInUser) } returns
        allocationDemandDetails
      coEvery { workforceAllocationsToDeliusApiClient.allocationDetails(crn, 2, STAFF_CODE, loggedInUser) } returns
        allocationDemandDetails
      coEvery { workforceAllocationsToDeliusApiClient.allocationDetails(crn, 3, STAFF_CODE, loggedInUser) } returns
        allocationDemandDetails

      coEvery { workforceAllocationsToDeliusApiClient.getAllocatedCaseView(crn) } returns
        allocatedCaseView

      val eventManagerEntity = SaveResult(EventManagerEntity(null, UUID.randomUUID(), crn, "", "", "", ZonedDateTime.now(), true, 1, null, "Bond"), true)
      coEvery { saveEventManagerService.saveEventManager("Reds", any(), any(), loggedInUser, STAFF_CODE, name.getCombinedName()) } returns
        eventManagerEntity

      val personManagerEntity = PersonManagerEntity(1L, uuid, crn, STAFF_CODE, STAFF_TEAM_CODE, loggedInUser, ZonedDateTime.now(), true, AllocationReason.RISK_TO_STAFF)
      coEvery { savePersonManagerService.savePersonManager("Reds", staffMember, loggedInUser, crn, AllocationReason.RISK_TO_STAFF) } returns
        SaveResult(personManagerEntity, true)

      val requirementManagerEntity = RequirementManagerEntity(1L, uuid, crn, BigInteger.valueOf(1L), STAFF_CODE, STAFF_TEAM_CODE, "someone", ZonedDateTime.now(), true, 1, AllocationReason.RISK_TO_STAFF)
      coEvery { saveRequirementManagerService.saveRequirementManagers(STAFF_TEAM_CODE, staffMember, allocateCase.crn, 1, AllocationReason.RISK_TO_STAFF, loggedInUser, any()) } returns
        listOf(
          SaveResult(requirementManagerEntity, true),
        )

      coEvery { telemetryService.trackRequirementManagerAllocated(requirementManagerEntity, caseDetails) } just Runs
      coEvery { telemetryService.trackPersonManagerAllocated(personManagerEntity, caseDetails) } just Runs
      coEvery { telemetryService.trackEventManagerAllocated(eventManagerEntity.entity, caseDetails) } just Runs
      coEvery { telemetryService.trackEventManagerAllocated(eventManagerEntity.entity, caseDetails) } just Runs
      coEvery { telemetryService.trackStaffGradeToTierAllocated(caseDetails, staffMember, STAFF_TEAM_CODE) } just Runs

      coEvery { sqsSuccessPublisher.updatePerson(crn, any(), any()) } just Runs
      coEvery { sqsSuccessPublisher.updateEvent(crn, any(), any()) } just Runs
      coEvery { sqsSuccessPublisher.updateRequirement(crn, any(), any()) } just Runs
      coEvery { sqsSuccessPublisher.auditAllocation(crn, any(), any(), any()) } just Runs
      coEvery { workforceAllocationsToDeliusApiClient.getOfficerView(PREVIOUS_STAFF_CODE) } returns OfficerView(PREVIOUS_STAFF_CODE, name, "SPO", null, BigInteger.ONE, BigInteger.ONE, BigInteger.ONE)

      val reallocationDetails = ReallocationDetails(
        toText(allocateCase.allocationReason!!),
        allocateCase.lastOasysAssessmentDate,
        allocateCase.nextAppointmentDate,
        allocateCase.failureToComply,
        StaffMember(PREVIOUS_STAFF_CODE, name, null, "SPO"),
      )

      coEvery { notificationService.notifyReallocation(allocationDemandDetails, allocateCase, caseDetails, reallocationDetails) } returns
        NotificationMessageResponse("template", "ref1", setOf("me@there.com"))

      val workload = defaultSaveWorkloadService.saveReallocatedWorkLoad(staffIdentifier, PREVIOUS_STAFF_CODE, allocateCase, loggedInUser)

      assertEquals(workload!!.eventManagerId, listOf(eventManagerEntity.entity.uuid, eventManagerEntity.entity.uuid, eventManagerEntity.entity.uuid))
      assertEquals(workload.requirementManagerIds, listOf(requirementManagerEntity.uuid, requirementManagerEntity.uuid, requirementManagerEntity.uuid))
      assertEquals(workload.personManagerId, personManagerEntity.uuid)

      coVerify(exactly = 1) { notificationService.notifyReallocation(allocationDemandDetails, allocateCase, caseDetails, reallocationDetails) }

      coVerify(exactly = 1) { sqsSuccessPublisher.updatePerson(crn, any(), any()) }
      coVerify(exactly = 3) { sqsSuccessPublisher.updateEvent(crn, any(), any()) }
      coVerify(exactly = 3) { sqsSuccessPublisher.updateRequirement(crn, any(), any()) }
      coVerify(exactly = 1) { sqsSuccessPublisher.auditAllocation(crn, any(), any(), any()) }

      coVerify(exactly = 1) { telemetryService.trackStaffGradeToTierAllocated(caseDetails, staffMember, STAFF_TEAM_CODE) }
      coVerify(exactly = 1) { telemetryService.trackPersonManagerAllocated(personManagerEntity, caseDetails) }
      coVerify(exactly = 3) { telemetryService.trackEventManagerAllocated(eventManagerEntity.entity, caseDetails) }
      coVerify(exactly = 3) { telemetryService.trackRequirementManagerAllocated(requirementManagerEntity, caseDetails) }
    }
  }

  private fun toText(reason: AllocationReason) = reason.toString().capitalize().replace("_", " ")
}
