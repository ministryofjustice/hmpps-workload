package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Case
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerCases
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerOverview
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerPotentialWorkload
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.TierCaseTotals
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.OverviewOffenderManager
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.OffenderManagerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WorkloadPointsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.service.CaseCalculator
import uk.gov.justice.digital.hmpps.hmppsworkload.service.GetWeeklyHours
import uk.gov.justice.digital.hmpps.hmppsworkload.service.calculateCapacity
import uk.gov.justice.digital.hmpps.hmppsworkload.service.reduction.GetReductionService
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime

@Service
class GetOffenderManagerService(
  private val offenderManagerRepository: OffenderManagerRepository,
  private val caseCalculator: CaseCalculator,
  private val getReductionService: GetReductionService,
  private val workloadPointsRepository: WorkloadPointsRepository,
  private val caseDetailsRepository: CaseDetailsRepository,
  private val getWeeklyHours: GetWeeklyHours,
  private val getEventManager: JpaBasedGetEventManager,
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {

  suspend fun getPotentialWorkload(staffIdentifier: StaffIdentifier, crn: String): OffenderManagerPotentialWorkload? {
    val impactResponse = workforceAllocationsToDeliusApiClient.impact(crn, staffIdentifier.staffCode)
    val potentialCase = getPotentialCase(crn)
    val overview = findOffenderManagerOverview(staffIdentifier, impactResponse.staff.getGrade())
    val currentCaseImpact = getCurrentCasePoints(staffIdentifier, potentialCase)

    overview.potentialCapacity = calculateCapacity(
      overview.totalPoints.minus(currentCaseImpact)
        .plus(caseCalculator.getPointsForCase(potentialCase)),
      overview.availablePoints,
    )
    return OffenderManagerPotentialWorkload.from(overview, impactResponse, potentialCase)
  }

  private fun getPotentialCase(crn: String): Case = caseDetailsRepository.findByIdOrNull(crn)!!
    .let { Case(tier = it.tier, type = it.type, crn = crn) }

  private fun getCurrentCasePoints(staffIdentifier: StaffIdentifier, case: Case): BigInteger = offenderManagerRepository.findCaseByTeamCodeAndStaffCodeAndCrn(staffIdentifier.teamCode, staffIdentifier.staffCode, case.crn)?.let {
    return caseCalculator.getPointsForCase(case)
  } ?: BigInteger.ZERO

  private fun getDefaultOffenderManagerOverview(staffCode: String, grade: String): OverviewOffenderManager {
    val workloadPoints = workloadPointsRepository.findFirstByIsT2AAndEffectiveToIsNullOrderByEffectiveFromDesc(false)
    val availablePoints = workloadPoints.getDefaultPointsAvailable(grade)
    val defaultContractedHours = workloadPoints.getDefaultContractedHours(grade)

    val overview = OverviewOffenderManager(0, 0, availablePoints.toBigInteger(), BigInteger.ZERO, staffCode, LocalDateTime.now(), -1, BigInteger.ZERO)
    overview.capacity = calculateCapacity(overview.totalPoints, overview.availablePoints)
    overview.contractedHours = defaultContractedHours
    return overview
  }

  suspend fun getOverview(staffIdentifier: StaffIdentifier): OffenderManagerOverview? {
    val officerView = workforceAllocationsToDeliusApiClient.getOfficerView(staffIdentifier.staffCode)
    val overview = findOffenderManagerOverview(staffIdentifier, officerView.getGrade())
    overview.lastAllocatedEvent = getEventManager.findLatestByStaffAndTeam(staffIdentifier)

    if (overview.hasWorkload) {
      overview.nextReductionChange = getReductionService.findNextReductionChange(staffIdentifier)
      overview.reductionHours = getReductionService.findReductionHours(staffIdentifier)
      overview.contractedHours = getWeeklyHours.findWeeklyHours(staffIdentifier, officerView.getGrade())
      offenderManagerRepository.findByCaseloadTotals(overview.workloadOwnerId).let { totals ->
        overview.tierCaseTotals = totals.map { total ->
          TierCaseTotals(total.getATotal(), total.getBTotal(), total.getCTotal(), total.getDTotal(), total.getASTotal(), total.getBSTotal(), total.getCSTotal(), total.getDSTotal(), total.untiered)
        }
          .fold(TierCaseTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)) { first, second -> TierCaseTotals(first.A.add(second.A), first.B.add(second.B), first.C.add(second.C), first.D.add(second.D), first.AS.add(second.AS), first.BS.add(second.BS), first.CS.add(second.CS), first.DS.add(second.DS), first.untiered.add(second.untiered)) }
      }
    }
    return OffenderManagerOverview.from(overview, officerView)
  }

  private fun findOffenderManagerOverview(staffIdentifier: StaffIdentifier, grade: String): OverviewOffenderManager = offenderManagerRepository.findByOverview(staffIdentifier.teamCode, staffIdentifier.staffCode)?.let {
    it.capacity = calculateCapacity(it.totalPoints, it.availablePoints)
    it.hasWorkload = true
    it
  } ?: getDefaultOffenderManagerOverview(staffIdentifier.staffCode, grade)

  suspend fun getCases(staffIdentifier: StaffIdentifier): OffenderManagerCases? = offenderManagerRepository.findCasesByTeamCodeAndStaffCode(staffIdentifier.staffCode, staffIdentifier.teamCode).let { cases ->
    val crnDetails = getCrnToCaseDetails(cases)
    val staffActiveCases = workforceAllocationsToDeliusApiClient.staffActiveCases(staffIdentifier.staffCode, crnDetails.keys)
    OffenderManagerCases.from(staffActiveCases, crnDetails)
  }

  private fun getCrnToCaseDetails(crns: List<String>): Map<String, CaseDetailsEntity> = if (crns.isEmpty()) emptyMap() else caseDetailsRepository.findAllById(crns).associateBy { it.crn }
}
