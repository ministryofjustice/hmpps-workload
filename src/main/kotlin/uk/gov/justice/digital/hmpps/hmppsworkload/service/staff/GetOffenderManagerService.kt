package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffActiveCases
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Case
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerCases
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerOverview
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerPotentialWorkload
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository

@Service
@Suppress("LongParameterList")
class GetOffenderManagerService(
  private val caseDetailsRepository: CaseDetailsRepository,
  private val getEventManager: JpaBasedGetEventManager,
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  private val caseTotalsService: CaseTotalsService,
) {

  suspend fun getPotentialWorkload(staffIdentifier: StaffIdentifier, crn: String): OffenderManagerPotentialWorkload? {
    val impactResponse = workforceAllocationsToDeliusApiClient.impact(crn, staffIdentifier.staffCode)
    val potentialCase = getPotentialCase(crn)

    return OffenderManagerPotentialWorkload.from(impactResponse, potentialCase)
  }

  private fun getPotentialCase(crn: String): Case = caseDetailsRepository.findByIdOrNull(crn)!!
    .let { Case(tier = it.tier, provisionalTier = it.provisionalTier, type = it.type, crn = crn) }

  suspend fun getOverview(staffIdentifier: StaffIdentifier): OffenderManagerOverview? {
    val officerView = workforceAllocationsToDeliusApiClient.getOfficerView(staffIdentifier.staffCode)
    val practitionerCases = workforceAllocationsToDeliusApiClient.staffActiveCases(staffIdentifier)

    val offenderManagerCases = mapDeliusCasesToCaseDetails(practitionerCases)
    val tierCaseTotals = caseTotalsService.getPractitionerTotalsByTier(practitionerCases.cases)
    val lastAllocatedEvent = getEventManager.findLatestByStaffAndTeam(staffIdentifier)

    return OffenderManagerOverview.from(offenderManagerCases, tierCaseTotals, lastAllocatedEvent, officerView)
  }

  suspend fun getCases(staffIdentifier: StaffIdentifier): OffenderManagerCases? = mapDeliusCasesToCaseDetails(
    workforceAllocationsToDeliusApiClient.staffActiveCases(staffIdentifier),
  )

  suspend fun getTeamCases(teamCode: String): Map<StaffIdentifier, OffenderManagerCases?> = workforceAllocationsToDeliusApiClient.teamActiveCases(teamCode).let { teamActiveCases ->
    teamActiveCases.staff
      .map { mapDeliusCasesToCaseDetails(it) }
      .associateBy { StaffIdentifier(it.code, teamCode) }
  }

  private fun mapDeliusCasesToCaseDetails(staffActiveCases: StaffActiveCases): OffenderManagerCases = if (staffActiveCases.cases.isEmpty()) {
    OffenderManagerCases.from(staffActiveCases)
  } else {
    OffenderManagerCases.from(staffActiveCases, getCrnToCaseDetails(staffActiveCases.cases.map { it.crn }))
  }

  private fun getCrnToCaseDetails(crns: List<String>): Map<String, CaseDetailsEntity> = if (crns.isEmpty()) emptyMap() else caseDetailsRepository.findAllById(crns).associateBy { it.crn }
}
