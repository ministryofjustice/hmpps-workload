package uk.gov.justice.digital.hmpps.hmppsworkload.service

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ActiveCase
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ImpactResponse
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Name
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.OfficerView
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffActiveCases
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.EventDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerActiveCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.TierCaseTotals
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.PersonManagerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.CaseTotalsService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.GetOffenderManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.JpaBasedGetEventManager
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.ZonedDateTime

private const val OFFICER_EMAIL = "me@here.com"
private const val OFFICER_GRADE = "SPO"
private const val OFFICER_CODE = "007"
private const val STAFF_CODE = "001"
private const val STAFF_TEAM_CODE = "Reds"

class GetOffenderManagerServiceTest {
  private val workforceAllocationsToDeliusApiClient = mockk<WorkforceAllocationsToDeliusApiClient>()
  private val caseDetailsRepository = mockk<CaseDetailsRepository>()
  private val getEventManager = mockk<JpaBasedGetEventManager>()
  private val caseTotalsService = mockk<CaseTotalsService>()
  private val personManagerRepository = mockk<PersonManagerRepository>()

  private val offenderManagerService = GetOffenderManagerService(
    caseDetailsRepository,
    getEventManager,
    workforceAllocationsToDeliusApiClient,
    caseTotalsService,
  )

  @Test
  fun `gets correct workload details`() = runBlocking {
    val crn = "1234"
    val name = Name("Jim", "A", "Bond")
    val staffIdentifier = StaffIdentifier(STAFF_CODE, STAFF_TEAM_CODE)
    val staffMember = StaffMember(OFFICER_CODE, name, OFFICER_EMAIL, OFFICER_GRADE)

    coEvery { workforceAllocationsToDeliusApiClient.impact(crn, staffIdentifier.staffCode) } returns ImpactResponse(crn, name, staffMember)
    coEvery { caseDetailsRepository.findByIdOrNull(crn) } returns CaseDetailsEntity(crn, Tier.A, false, CaseType.CUSTODY, "John", "Smith")

    val workload = offenderManagerService.getPotentialWorkload(staffIdentifier, crn)
    assertEquals(workload?.name, name)
    assertEquals(workload?.staff, staffMember)
    assertEquals(workload?.tier, Tier.A)
  }

  @Test
  fun `gets correct overview details`() = runBlocking {
    val crn = "1234"
    val name = Name("Jim", "A", "Bond")
    val staffIdentifier = StaffIdentifier(STAFF_CODE, STAFF_TEAM_CODE)
    val eventDetails = EventDetails(Tier.A, false, CaseType.CUSTODY, crn, ZonedDateTime.now())
    val activeCases = listOf(ActiveCase(crn, name, "CUSTODY", LocalDate.now()))

    coEvery { workforceAllocationsToDeliusApiClient.getOfficerView(staffIdentifier.staffCode) } returns OfficerView(
      STAFF_CODE,
      name,
      OFFICER_GRADE,
      OFFICER_EMAIL,
      BigInteger.valueOf(4),
      BigInteger.valueOf(5),
      BigInteger.valueOf(6),
    )
    coEvery { caseDetailsRepository.findAllById(listOf(crn)) } returns listOf(CaseDetailsEntity(crn, Tier.A, false, CaseType.CUSTODY, "John", "Smith"))
    coEvery { getEventManager.findLatestByStaffAndTeam(staffIdentifier) } returns eventDetails
    coEvery { workforceAllocationsToDeliusApiClient.staffActiveCases(staffIdentifier) } returns StaffActiveCases("", Name("", "", ""), "", "", activeCases)
    coEvery { caseTotalsService.getPractitionerTotalsByTier(any()) } returns TierCaseTotals(BigDecimal.valueOf(120), BigDecimal.valueOf(122), BigDecimal.valueOf(124), BigDecimal.valueOf(126), BigDecimal.valueOf(128), BigDecimal.valueOf(130), BigDecimal.valueOf(132), BigDecimal.valueOf(134), BigDecimal.valueOf(25))

    val overview = offenderManagerService.getOverview(staffIdentifier)

    assertEquals(overview?.code, STAFF_CODE)
    assertEquals(overview?.email, OFFICER_EMAIL)
    assertEquals(overview?.grade, OFFICER_GRADE)
    assertEquals(overview?.caseTotals?.a, BigDecimal.valueOf(120))
    assertEquals(overview?.caseTotals?.b, BigDecimal.valueOf(122))
    assertEquals(overview?.caseTotals?.c, BigDecimal.valueOf(124))
    assertEquals(overview?.caseTotals?.d, BigDecimal.valueOf(126))
    assertEquals(overview?.caseTotals?.e, BigDecimal.valueOf(128))
    assertEquals(overview?.caseTotals?.f, BigDecimal.valueOf(130))
    assertEquals(overview?.caseTotals?.g, BigDecimal.valueOf(132))
    assertEquals(overview?.caseTotals?.missing, BigDecimal.valueOf(134))
    assertEquals(overview?.caseTotals?.notSupervised, BigDecimal.valueOf(25))
    assertEquals(overview?.caseEndDue, BigInteger.valueOf(4))
    assertEquals(overview?.totalCases, 1)
  }

  @Test
  fun `gets cases correctly`() = runBlocking {
    val crn = "1234"
    val name = Name("Jim", "A", "Bond")
    val staffIdentifier = StaffIdentifier(STAFF_CODE, STAFF_TEAM_CODE)

    val now = LocalDate.now()

    coEvery { workforceAllocationsToDeliusApiClient.staffActiveCases(staffIdentifier) } returns StaffActiveCases("002", name, OFFICER_GRADE, OFFICER_EMAIL, listOf(ActiveCase(crn, name, "CUSTODY", now)))

    val caseDetailsEntity = CaseDetailsEntity(crn, Tier.A, false, CaseType.CUSTODY, "John", "Smith")
    coEvery { caseDetailsRepository.findAllById(listOf(crn)) } returns listOf(caseDetailsEntity)

    val personManagerEntity = PersonManagerEntity(crn = crn, teamCode = STAFF_TEAM_CODE, staffCode = STAFF_CODE, createdBy = "USER.NAME", isActive = true, allocationReason = AllocationReason.INITIAL_ALLOCATION)
    coEvery { personManagerRepository.findByStaffCodeAndTeamCodeAndIsActiveIsTrue(STAFF_CODE, STAFF_TEAM_CODE) } returns listOf(personManagerEntity)

    val cases = offenderManagerService.getCases(staffIdentifier)
    assertEquals(cases?.name, name)
    assertEquals(cases?.code, "002")
    assertEquals(cases?.grade, "SPO")
    assertEquals(cases?.activeCases, listOf(OffenderManagerActiveCase(crn, Tier.A.toString(), false, name, "CUSTODY", now)))
  }

  @Test
  fun `gets cases correctly when delius returns extra cases`() = runBlocking {
    val crn1 = "1234"
    val crn2 = "2345"
    val name = Name("Jim", "A", "Bond")

    val staffIdentifier = StaffIdentifier(STAFF_CODE, STAFF_TEAM_CODE)

    val now = LocalDate.now()

    coEvery { workforceAllocationsToDeliusApiClient.staffActiveCases(staffIdentifier) } returns StaffActiveCases("002", name, OFFICER_GRADE, OFFICER_EMAIL, listOf(ActiveCase(crn1, name, "CUSTODY", now), ActiveCase(crn2, name, "CUSTODY", now)))

    val caseDetailsEntity = CaseDetailsEntity(crn1, Tier.A, false, CaseType.CUSTODY, "John", "Smith")
    coEvery { caseDetailsRepository.findAllById(listOf(crn1, crn2)) } returns listOf(caseDetailsEntity)

    val personManagerEntity = PersonManagerEntity(crn = crn1, teamCode = STAFF_TEAM_CODE, staffCode = STAFF_CODE, createdBy = "USER.NAME", isActive = true, allocationReason = AllocationReason.INITIAL_ALLOCATION)
    coEvery { personManagerRepository.findByStaffCodeAndTeamCodeAndIsActiveIsTrue(STAFF_CODE, STAFF_TEAM_CODE) } returns listOf(personManagerEntity)

    val cases = offenderManagerService.getCases(staffIdentifier)
    assertEquals(cases?.name, name)
    assertEquals(cases?.code, "002")
    assertEquals(cases?.grade, "SPO")
    assertEquals(cases?.activeCases, listOf(OffenderManagerActiveCase(crn1, Tier.A.toString(), false, name, "CUSTODY", now)))
  }
}
