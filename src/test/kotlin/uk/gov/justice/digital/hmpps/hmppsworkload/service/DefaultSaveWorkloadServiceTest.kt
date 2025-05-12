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
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocationDemandDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Court
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.InitialAppointment
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Manager
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Name
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.OffenceDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Requirement
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskOGRS
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.SentenceDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
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
import java.math.BigInteger
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

private const val OFFICER_EMAIL = "me@here.com"
private const val OFFICER_GRADE = "SPO"
private const val OFFICER_CODE = "007"
private const val STAFF_CODE = "001"
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
      val allocateCase = AllocateCase(crn, instructions = "", emailTo = null, sendEmailCopyToAllocatingOfficer = false, eventNumber = 1, allocationJustificationNotes = "Some Notes", sensitiveNotes = false, spoOversightNotes = "spo notes", sensitiveOversightNotes = null, laoCase = false)

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

      val personManagerEntity = PersonManagerEntity(1L, uuid, crn, STAFF_CODE, STAFF_TEAM_CODE, loggedInUser, ZonedDateTime.now(), true)
      coEvery { savePersonManagerService.savePersonManager("Reds", staffMember, loggedInUser, crn) } returns
        SaveResult(personManagerEntity, true)

      val requirementManagerEntity = RequirementManagerEntity(1L, uuid, crn, BigInteger.valueOf(1L), STAFF_CODE, STAFF_TEAM_CODE, "someone", ZonedDateTime.now(), true, 1)
      coEvery { saveRequirementManagerService.saveRequirementManagers(STAFF_TEAM_CODE, staffMember, allocateCase, loggedInUser, any()) } returns
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

      assertEquals(workload.eventManagerId, eventManagerEntity.entity.uuid)
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
      coVerify(exactly = 1) { telemetryService.trackStaffGradeToTierAllocated(caseDetails, staffMember, STAFF_TEAM_CODE) }
    }
  }
}
