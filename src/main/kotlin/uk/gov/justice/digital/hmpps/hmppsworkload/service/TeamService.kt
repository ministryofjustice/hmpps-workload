package uk.gov.justice.digital.hmpps.hmppsworkload.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.HmppsProbationEstateApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Practitioner
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.PractitionerStats
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.PractitionerWithRawWorkloadPoints
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.PractitionerWorkload
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.TierCaseTotals
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.WorkloadCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.ReportPractitionerData
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.ReportPractitionerId
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.service.powerbi.ReportDataService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.CaseTotalsService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.GetOffenderManagerService
import java.time.LocalDate
import java.time.ZoneId

private const val CASE_COUNT_PERIOD_DAYS = 7L

@Service
class TeamService(
  private val caseDetailsRepository: CaseDetailsRepository,
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  private val hmppsProbationEstateApiClient: HmppsProbationEstateApiClient,
  private val reportDataService: ReportDataService,
  private val caseTotalsService: CaseTotalsService,
  private val offenderManagerService: GetOffenderManagerService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  suspend fun getPractitioners(teamCodes: List<String>, crn: String, grades: List<String>?): PractitionerWorkload? {
    return workforceAllocationsToDeliusApiClient.choosePractitioners(crn, teamCodes)?.let { choosePractitionerResponse ->
      val caseCountAfter = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(CASE_COUNT_PERIOD_DAYS)
      val practitionerAllocationCaseCounts = caseTotalsService.getPractitionerAllocationCaseCounts(teamCodes, caseCountAfter)
      val practitionerReallocationCaseCounts = caseTotalsService.getPractitionerReallocationCaseCounts(teamCodes, caseCountAfter)

      val teamNames = hmppsProbationEstateApiClient.getTeams(teamCodes).associate { it.code to it.name }
      val reportPractitionerData = reportDataService.getPractitionerData(teamNames.values.toList())
      val teamTierTotals = caseTotalsService.getTeamPractitionerTotalsByTier(teamCodes)

      val enrichedTeams = choosePractitionerResponse.teams.mapValues { team ->
        team.value
          .filter { grades == null || grades.contains(it.getGrade()) }
          .map {
            val teamStaffId = teamStaffId(team.key, it.code)
            val practitionerCases = offenderManagerService.getCases(StaffIdentifier(it.code, team.key))!!

            val reportPractitionerId = getReportPractitionerId(teamNames, team.key, it)
            val practitionerStats = getPractitionerStats(
              practitionerAllocationCaseCounts,
              practitionerReallocationCaseCounts,
              reportPractitionerData,
              teamStaffId,
              reportPractitionerId,
              teamTierTotals[teamStaffId],
            )

            Practitioner.from(it, practitionerCases, practitionerStats)
          }
      }

      return caseDetailsRepository.findByIdOrNull(crn)?.let {
        PractitionerWorkload.from(
          choosePractitionerResponse,
          it.tier,
          it.provisionalTier,
          enrichedTeams,
        )
      }
    }
  }

  private fun teamStaffId(teamCode: String, staffCode: String) = "$teamCode-$staffCode"

  suspend fun getWorkloadCases(teams: List<String>): Flow<WorkloadCase> {
    val activeTotals = caseTotalsService.getTeamTotals(teams)

    val teamNames = hmppsProbationEstateApiClient.getTeams(teams).associate { it.code to it.name }
    val suspendedTotals = reportDataService.getTeamContactSuspendedCases(teamNames.values.toList())

    val totals = teams.map {
      val active = activeTotals.getOrDefault(it, 0)
      val suspended = suspendedTotals.getOrDefault(teamNames[it].orEmpty(), 0)

      WorkloadCase(it, active + suspended)
    }

    return totals.asFlow()
  }

  suspend fun getPractitioners(teamCodes: List<String>): Map<String, List<PractitionerWithRawWorkloadPoints>>? {
    return workforceAllocationsToDeliusApiClient.choosePractitioners(teamCodes)?.let { choosePractitionerResponse ->
      val caseCountAfter = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(CASE_COUNT_PERIOD_DAYS)
      val practitionerAllocationCaseCounts = caseTotalsService.getPractitionerAllocationCaseCountsTeamCodeOnly(teamCodes, caseCountAfter)
      val practitionerReallocationCaseCounts = caseTotalsService.getPractitionerReallocationCaseCountsTeamCodeOnly(teamCodes, caseCountAfter)

      val teamNames = hmppsProbationEstateApiClient.getTeams(teamCodes).associate { it.code to it.name }
      val reportPractitionerData = reportDataService.getPractitionerData(teamNames.values.toList())
      val teamTierTotals = caseTotalsService.getTeamPractitionerTotalsByTier(teamCodes)

      log.info("Practitioner Allocation Case Counts: $practitionerAllocationCaseCounts")
      log.info("Practitioner Reallocation Case Counts: $practitionerReallocationCaseCounts")

      return choosePractitionerResponse.teams.mapValues { team ->
        team.value.map {
          val teamStaffId = it.code
          log.info("StaffId to get workload: $teamStaffId")
          val practitionerCases = offenderManagerService.getCases(StaffIdentifier(it.code, team.key))!!

          val reportPractitionerId = getReportPractitionerId(teamNames, team.key, it)
          val practitionerStats = getPractitionerStats(practitionerAllocationCaseCounts, practitionerReallocationCaseCounts, reportPractitionerData, teamStaffId, reportPractitionerId, teamTierTotals[teamStaffId(team.key, it.code)])

          PractitionerWithRawWorkloadPoints.from(it, practitionerCases, practitionerStats)
        }
      }
    }
  }

  private fun getReportPractitionerId(
    teamNames: Map<String, String>,
    teamCode: String,
    member: StaffMember,
  ): ReportPractitionerId {
    val teamName = teamNames[teamCode]
    val practitionerName = "${member.name.surname}, ${member.name.forename}"
    val reportPractitionerId = ReportPractitionerId(teamName.orEmpty(), practitionerName)
    return reportPractitionerId
  }

  private suspend fun getPractitionerStats(
    practitionerAllocationCaseCounts: Map<String, Int>,
    practitionerReallocationCaseCounts: Map<String, Int>,
    reportPractitionerData: ReportPractitionerData,
    teamStaffId: String,
    reportPractitionerId: ReportPractitionerId,
    tierCaseTotals: TierCaseTotals?,
  ): PractitionerStats = PractitionerStats(
    practitionerAllocationCaseCounts.getOrDefault(teamStaffId, 0),
    practitionerReallocationCaseCounts.getOrDefault(teamStaffId, 0),
    reportPractitionerData.ispsDueInNext14Days.getOrDefault(reportPractitionerId, 0),
    reportPractitionerData.contactSuspendedCases.getOrDefault(reportPractitionerId, 0),
    reportPractitionerData.custodyReleasesInNext7Days.getOrDefault(reportPractitionerId, 0),
    reportPractitionerData.paroleReportsInNext28Days.getOrDefault(reportPractitionerId, 0),
    reportPractitionerData.hdcrotlReportsInNext14Days.getOrDefault(reportPractitionerId, 0),
    reportPractitionerData.partBReportsInNext14Days.getOrDefault(reportPractitionerId, 0),
    reportPractitionerData.partCReportsInNext14Days.getOrDefault(reportPractitionerId, 0),
    tierCaseTotals ?: TierCaseTotals(),
  )
}
