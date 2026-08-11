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
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.EventDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerActiveCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.TierCaseTotals
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.OverviewOffenderManager
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.OffenderManagerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WorkloadPointsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.service.reduction.GetReductionService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.CaseTotalsService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.GetOffenderManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.JpaBasedGetEventManager
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

private const val OFFICER_EMAIL = "me@here.com"
private const val OFFICER_GRADE = "SPO"
private const val OFFICER_CODE = "007"
private const val STAFF_CODE = "001"
private const val STAFF_TEAM_CODE = "Reds"

class GetOffenderManagerServiceTest {
  private val workforceAllocationsToDeliusApiClient = mockk<WorkforceAllocationsToDeliusApiClient>()
  private val workloadPointsRepository = mockk<WorkloadPointsRepository>()
  private val offenderManagerRepository = mockk<OffenderManagerRepository>()
  private val getReductionService = mockk<GetReductionService>()
  private val caseDetailsRepository = mockk<CaseDetailsRepository>()
  private val getWeeklyHours = mockk<GetWeeklyHours>()
  private val getEventManager = mockk<JpaBasedGetEventManager>()
  private val caseTotalsService = mockk<CaseTotalsService>()

  private val offenderManagerService = GetOffenderManagerService(
    offenderManagerRepository,
    getReductionService,
    workloadPointsRepository,
    caseDetailsRepository,
    getWeeklyHours,
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
    val communityCases = 1L
    val licenseCases = 0L
    val custodyCases = 2L
    val availablePoints = 102L
    val totalPoints = BigInteger.valueOf(15)

    coEvery { workforceAllocationsToDeliusApiClient.impact(crn, staffIdentifier.staffCode) } returns ImpactResponse(crn, name, staffMember)
    coEvery { caseDetailsRepository.findByIdOrNull(crn) } returns CaseDetailsEntity(crn, Tier.A, false, CaseType.CUSTODY, "John", "Smith")
    coEvery { offenderManagerRepository.findByOverview(STAFF_TEAM_CODE, STAFF_CODE) } returns OverviewOffenderManager(
      communityCases,
      licenseCases,
      custodyCases,
      BigInteger.valueOf(availablePoints),
      totalPoints,
      STAFF_CODE,
      LocalDateTime.now(),
      13,
      BigInteger.valueOf(12L),
    )
    coEvery { offenderManagerRepository.findCaseByTeamCodeAndStaffCodeAndCrn(STAFF_TEAM_CODE, STAFF_CODE, crn) } returns "002"

    val workload = offenderManagerService.getPotentialWorkload(staffIdentifier, crn)
    assertEquals(workload?.name, name)
    assertEquals(workload?.staff, staffMember)
    assertEquals(workload?.capacity, BigDecimal("14.700"))
    assertEquals(workload?.potentialCapacity, BigDecimal("14.700"))
    assertEquals(workload?.tier, Tier.A)
  }

  @Test
  fun `gets correct overview details`() = runBlocking {
    val crn = "1234"
    val name = Name("Jim", "A", "Bond")
    val staffIdentifier = StaffIdentifier(STAFF_CODE, STAFF_TEAM_CODE)
    val eventDetails = EventDetails(Tier.A, false, CaseType.CUSTODY, crn, ZonedDateTime.now())
    val reductionHours = BigDecimal.valueOf(1.5)
    val workWeekHours = BigDecimal.valueOf(40)
    val totalPoints = BigInteger.valueOf(15)
    val communityCases = 1L
    val licenseCases = 0L
    val custodyCases = 2L
    val availablePoints = 102L

    coEvery { workforceAllocationsToDeliusApiClient.getOfficerView(staffIdentifier.staffCode) } returns OfficerView(
      STAFF_CODE,
      name,
      OFFICER_GRADE,
      OFFICER_EMAIL,
      BigInteger.valueOf(4),
      BigInteger.valueOf(5),
      BigInteger.valueOf(6),
    )
    coEvery { caseDetailsRepository.findByIdOrNull(crn) } returns CaseDetailsEntity(crn, Tier.A, false, CaseType.CUSTODY, "John", "Smith")
    coEvery { offenderManagerRepository.findByOverview(STAFF_TEAM_CODE, STAFF_CODE) } returns OverviewOffenderManager(
      communityCases,
      licenseCases,
      custodyCases,
      BigInteger.valueOf(availablePoints),
      totalPoints,
      STAFF_CODE,
      LocalDateTime.now(),
      13,
      BigInteger.valueOf(12L),
    )
    coEvery { offenderManagerRepository.findCaseByTeamCodeAndStaffCodeAndCrn(STAFF_TEAM_CODE, STAFF_CODE, "1234") } returns "002"
    coEvery { getEventManager.findLatestByStaffAndTeam(staffIdentifier) } returns eventDetails
    coEvery { getReductionService.findNextReductionChange(staffIdentifier) } returns ZonedDateTime.now().plusDays(1)
    coEvery { getReductionService.findReductionHours(staffIdentifier) } returns reductionHours
    coEvery { getWeeklyHours.findWeeklyHours(staffIdentifier, OFFICER_GRADE) } returns workWeekHours
    coEvery { caseTotalsService.getPractitionerTotalsByTier(staffIdentifier.staffCode, staffIdentifier.teamCode) } returns TierCaseTotals(BigDecimal.valueOf(120), BigDecimal.valueOf(122), BigDecimal.valueOf(124), BigDecimal.valueOf(126), BigDecimal.valueOf(128), BigDecimal.valueOf(130), BigDecimal.valueOf(132), BigDecimal.valueOf(134), BigDecimal.valueOf(25))

    val overview = offenderManagerService.getOverview(staffIdentifier)

    assertEquals(overview?.code, STAFF_CODE)
    assertEquals(overview?.weeklyHours, workWeekHours)
    assertEquals(overview?.capacity?.compareTo(BigDecimal.valueOf(14.7)), 0)
    assertEquals(overview?.email, OFFICER_EMAIL)
    assertEquals(overview?.grade, OFFICER_GRADE)
    assertEquals(overview?.caseTotals?.A, BigDecimal.valueOf(120))
    assertEquals(overview?.caseTotals?.B, BigDecimal.valueOf(122))
    assertEquals(overview?.caseTotals?.C, BigDecimal.valueOf(124))
    assertEquals(overview?.caseTotals?.D, BigDecimal.valueOf(126))
    assertEquals(overview?.caseTotals?.E, BigDecimal.valueOf(128))
    assertEquals(overview?.caseTotals?.F, BigDecimal.valueOf(130))
    assertEquals(overview?.caseTotals?.G, BigDecimal.valueOf(132))
    assertEquals(overview?.caseTotals?.missing, BigDecimal.valueOf(134))
    assertEquals(overview?.caseTotals?.notSupervised, BigDecimal.valueOf(25))
    assertEquals(overview?.caseEndDue, BigInteger.valueOf(4))
    assertEquals(overview?.pointsAvailable, BigInteger.valueOf(availablePoints))
    assertEquals(overview?.pointsUsed, totalPoints)
    assertEquals(overview?.totalCases, communityCases + custodyCases)
    assertEquals(overview?.totalReductionHours, reductionHours)
  }

  @Test
  fun `gets cases correctly`() = runBlocking {
    val crn = "1234"
    val name = Name("Jim", "A", "Bond")
    val staffIdentifier = StaffIdentifier(STAFF_CODE, STAFF_TEAM_CODE)

    val now = LocalDate.now()

    coEvery { workforceAllocationsToDeliusApiClient.staffActiveCases(staffIdentifier.staffCode, any()) } returns StaffActiveCases("002", name, OFFICER_GRADE, OFFICER_EMAIL, listOf(ActiveCase(crn, name, "CUSTODY", now)))

    val caseDetailsEntity = CaseDetailsEntity(crn, Tier.A, false, CaseType.CUSTODY, "John", "Smith")
    coEvery { caseDetailsRepository.findAllById(listOf("002")) } returns listOf(caseDetailsEntity)

    coEvery { offenderManagerRepository.findCasesByTeamCodeAndStaffCode(STAFF_CODE, STAFF_TEAM_CODE) } returns listOf("002")

    val cases = offenderManagerService.getCases(staffIdentifier)
    assertEquals(cases?.name, name)
    assertEquals(cases?.code, "002")
    assertEquals(cases?.grade, "SPO")
    assertEquals(cases?.activeCases, listOf(OffenderManagerActiveCase(crn, Tier.A.toString(), false, name, "CUSTODY", now)))
  }
}
