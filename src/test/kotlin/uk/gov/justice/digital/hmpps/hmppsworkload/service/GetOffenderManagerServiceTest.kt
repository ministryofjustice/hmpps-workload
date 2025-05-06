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
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Case
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.EventDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerActiveCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.OffenderManagerCaseloadTotals
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.OverviewOffenderManager
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.OffenderManagerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WorkloadPointsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.service.reduction.GetReductionService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.GetOffenderManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.JpaBasedGetEventManager
import java.math.BigDecimal
import java.math.BigInteger
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
  private val caseCalculator = mockk<CaseCalculator>()
  private val getReductionService = mockk<GetReductionService>()
  private val caseDetailsRepository = mockk<CaseDetailsRepository>()
  private val getWeeklyHours = mockk<GetWeeklyHours>()
  private val getEventManager = mockk<JpaBasedGetEventManager>()

  private val offenderManagerService = GetOffenderManagerService(
    offenderManagerRepository,
    caseCalculator,
    getReductionService,
    workloadPointsRepository,
    caseDetailsRepository,
    getWeeklyHours,
    getEventManager,
    workforceAllocationsToDeliusApiClient,
  )

  @Test
  fun `gets correct workload details`() = runBlocking {
    val crn = "1234"
    val name = Name("Jim", "A", "Bond")
    val staffIdentifier = StaffIdentifier(STAFF_CODE, STAFF_TEAM_CODE)
    val staffMember = StaffMember(OFFICER_CODE, name, OFFICER_EMAIL, OFFICER_GRADE)
    val communityCases = 1L
    val custodyCases = 2L
    val availablePoints = 102L
    val totalPoints = BigInteger.valueOf(15)

    coEvery { workforceAllocationsToDeliusApiClient.impact(crn, staffIdentifier.staffCode) } returns ImpactResponse(crn, name, staffMember)
    coEvery { caseDetailsRepository.findByIdOrNull(crn) } returns CaseDetailsEntity(crn, Tier.A1, CaseType.CUSTODY, "John", "Smith")
    coEvery { offenderManagerRepository.findByOverview(STAFF_TEAM_CODE, STAFF_CODE) } returns OverviewOffenderManager(
      communityCases,
      custodyCases,
      BigInteger.valueOf(availablePoints),
      totalPoints,
      STAFF_CODE,
      LocalDateTime.now(),
      13,
      BigInteger.valueOf(12L),
    )
    coEvery { offenderManagerRepository.findCaseByTeamCodeAndStaffCodeAndCrn(STAFF_TEAM_CODE, STAFF_CODE, crn) } returns "002"
    coEvery { caseCalculator.getPointsForCase(Case(Tier.A1, CaseType.CUSTODY, false, crn)) } returns BigInteger.valueOf(12)

    val workload = offenderManagerService.getPotentialWorkload(staffIdentifier, crn)
    assertEquals(workload?.name, name)
    assertEquals(workload?.staff, staffMember)
    assertEquals(workload?.capacity, BigDecimal("14.700"))
    assertEquals(workload?.potentialCapacity, BigDecimal("14.700"))
    assertEquals(workload?.tier, Tier.A1)
  }

  @Test
  fun `gets correct overview details`() = runBlocking {
    val crn = "1234"
    val name = Name("Jim", "A", "Bond")
    val staffIdentifier = StaffIdentifier(STAFF_CODE, STAFF_TEAM_CODE)
    val eventDetails = EventDetails(Tier.A1, CaseType.CUSTODY, crn, ZonedDateTime.now())
    val reductionHours = BigDecimal.valueOf(1.5)
    val workWeekHours = BigDecimal.valueOf(40)
    val totalPoints = BigInteger.valueOf(15)
    val communityCases = 1L
    val custodyCases = 2L
    val availablePoints = 102L
    val caseLoadTotals = listOf(
      OffenderManagerCaseloadTotals(
        "home", BigDecimal.valueOf(12),
        BigDecimal.valueOf(1), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(2), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(3), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(4), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(5), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(6), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(7), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(8), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
      ),
      OffenderManagerCaseloadTotals(
        "away", BigDecimal.valueOf(13),
        BigDecimal.valueOf(101), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(102), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(103), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(104), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(105), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(106), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(107), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
        BigDecimal.valueOf(108), BigDecimal.valueOf(2), BigDecimal.valueOf(3), BigDecimal.valueOf(4),
      ),
    )

    coEvery { workforceAllocationsToDeliusApiClient.getOfficerView(staffIdentifier.staffCode) } returns OfficerView(
      STAFF_CODE,
      name,
      OFFICER_GRADE,
      OFFICER_EMAIL,
      BigInteger.valueOf(4),
      BigInteger.valueOf(5),
      BigInteger.valueOf(6),
    )
    coEvery { caseDetailsRepository.findByIdOrNull(crn) } returns CaseDetailsEntity(crn, Tier.A1, CaseType.CUSTODY, "John", "Smith")
    coEvery { offenderManagerRepository.findByOverview(STAFF_TEAM_CODE, STAFF_CODE) } returns OverviewOffenderManager(
      communityCases,
      custodyCases,
      BigInteger.valueOf(availablePoints),
      totalPoints,
      STAFF_CODE,
      LocalDateTime.now(),
      13,
      BigInteger.valueOf(12L),
    )
    coEvery { offenderManagerRepository.findCaseByTeamCodeAndStaffCodeAndCrn(STAFF_TEAM_CODE, STAFF_CODE, "1234") } returns "002"
    coEvery { caseCalculator.getPointsForCase(Case(Tier.A1, CaseType.CUSTODY, false, crn)) } returns BigInteger.valueOf(12)
    coEvery { getEventManager.findLatestByStaffAndTeam(staffIdentifier) } returns eventDetails
    coEvery { getReductionService.findNextReductionChange(staffIdentifier) } returns ZonedDateTime.now().plusDays(1)
    coEvery { getReductionService.findReductionHours(staffIdentifier) } returns reductionHours
    coEvery { getWeeklyHours.findWeeklyHours(staffIdentifier, OFFICER_GRADE) } returns workWeekHours
    coEvery { offenderManagerRepository.findByCaseloadTotals(13) } returns caseLoadTotals

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
    assertEquals(overview?.caseTotals?.AS, BigDecimal.valueOf(128))
    assertEquals(overview?.caseTotals?.BS, BigDecimal.valueOf(130))
    assertEquals(overview?.caseTotals?.CS, BigDecimal.valueOf(132))
    assertEquals(overview?.caseTotals?.DS, BigDecimal.valueOf(134))
    assertEquals(overview?.caseTotals?.untiered, BigDecimal.valueOf(25))
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

    coEvery { workforceAllocationsToDeliusApiClient.staffActiveCases(staffIdentifier.staffCode, any()) } returns StaffActiveCases("002", name, OFFICER_GRADE, OFFICER_EMAIL, listOf(ActiveCase(crn, name, "CUSTODY")))

    val caseDetailsEntity = CaseDetailsEntity(crn, Tier.A1, CaseType.CUSTODY, "John", "Smith")
    coEvery { caseDetailsRepository.findAllById(listOf("002")) } returns listOf(caseDetailsEntity)

    coEvery { offenderManagerRepository.findCasesByTeamCodeAndStaffCode(STAFF_CODE, STAFF_TEAM_CODE) } returns listOf("002")

    val cases = offenderManagerService.getCases(staffIdentifier)
    assertEquals(cases?.name, name)
    assertEquals(cases?.code, "002")
    assertEquals(cases?.grade, "SPO")
    assertEquals(cases?.activeCases, listOf(OffenderManagerActiveCase(crn, Tier.A1.toString(), name, "CUSTODY")))
  }
}
