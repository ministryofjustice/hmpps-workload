package uk.gov.justice.digital.hmpps.hmppsworkload.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Practitioner
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.PractitionerWorkload
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.WorkloadCase
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.TeamOverview
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.PersonManagerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.TeamRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WorkloadPointsRepository
import java.math.BigInteger
import java.time.LocalDate
import java.time.ZoneId

private const val CASE_COUNT_PERIOD_DAYS = 7L

@Service
class TeamService(
  private val teamRepository: TeamRepository,
  private val workloadPointsRepository: WorkloadPointsRepository,
  private val personManagerRepository: PersonManagerRepository,
  private val caseDetailsRepository: CaseDetailsRepository,
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  suspend fun getPractitioners(teamCodes: List<String>, crn: String, grades: List<String>?): PractitionerWorkload? {
    return workforceAllocationsToDeliusApiClient.choosePractitioners(crn, teamCodes)?.let { choosePractitionerResponse ->
      val practitionerWorkloads = teamRepository.findAllByTeamCodes(teamCodes).associateBy { teamStaffId(it.teamCode, it.staffCode) }
      val caseCountAfter = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(CASE_COUNT_PERIOD_DAYS)
      val practitionerCaseCounts = personManagerRepository.findByTeamCodeInAndCreatedDateGreaterThanEqualAndIsActiveIsTrue(teamCodes, caseCountAfter)
        .groupBy { teamStaffId(it.teamCode, it.staffCode) }
        .mapValues { countEntry -> countEntry.value.size }

      val enrichedTeams = choosePractitionerResponse.teams.mapValues { team ->
        team.value
          .filter { grades == null || grades.contains(it.getGrade()) }
          .map {
            val teamStaffId = teamStaffId(team.key, it.code)
            val practitionerWorkload = practitionerWorkloads[teamStaffId]
              ?: getTeamOverviewForOffenderManagerWithoutWorkload(it.code, it.getGrade(), team.key)
            Practitioner.from(it, practitionerWorkload, practitionerCaseCounts.getOrDefault(teamStaffId, 0))
          }
      }

      return caseDetailsRepository.findByIdOrNull(crn)?.let {
        PractitionerWorkload.from(
          choosePractitionerResponse,
          it.tier,
          enrichedTeams,
        )
      }
    }
  }

  private fun teamStaffId(teamCode: String, staffCode: String) = "$teamCode-$staffCode"

  suspend fun getWorkloadCases(teams: List<String>): Flow<WorkloadCase> = teamRepository.findWorkloadCountCaseByCode(teams).map {
    WorkloadCase(it.teamCode, it.totalCases, calculateCapacity(it.totalPoints.toBigInteger(), it.availablePoints.toBigInteger()).toDouble())
  }.asFlow()

  private fun getTeamOverviewForOffenderManagerWithoutWorkload(
    staffCode: String,
    grade: String,
    teamCode: String,
  ): TeamOverview = TeamOverview(
    0,
    0,
    defaultAvailablePointsForGrade(grade),
    BigInteger.ZERO,
    staffCode,
    teamCode,
  )

  private fun defaultAvailablePointsForGrade(grade: String): BigInteger {
    val workloadPoints = workloadPointsRepository.findFirstByIsT2AAndEffectiveToIsNullOrderByEffectiveFromDesc(false)
    return workloadPoints.getDefaultPointsAvailable(grade).toBigInteger()
  }

  suspend fun getPractitioners(teamCodes: List<String>, grades: List<String>?): Map<String, List<Practitioner>>? {
    return workforceAllocationsToDeliusApiClient.choosePractitioners(teamCodes)?.let { choosePractitionerResponse ->
      val practitionerWorkloads = teamRepository.findAllByTeamCodes(teamCodes).associateBy { it.staffCode }
      val caseCountAfter = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).minusDays(CASE_COUNT_PERIOD_DAYS)
      val practitionerCaseCounts = personManagerRepository.findByTeamCodeInAndCreatedDateGreaterThanEqualAndIsActiveIsTrue(teamCodes, caseCountAfter)
        .groupBy { it.staffCode }
        .mapValues { countEntry -> countEntry.value.size }
      log.info("Practitioner Workloads: $practitionerWorkloads")
      log.info("Practitioner Case Counts: $practitionerCaseCounts")

      return choosePractitionerResponse.teams.mapValues { team ->
        team.value
          .filter { grades == null || grades.contains(it.retrieveGrade()) }
          .map {
            val teamStaffId = it.code
            log.info("StaffId to get workload: $teamStaffId")
            log.info("Practitioner Workload: ${practitionerWorkloads[teamStaffId]}")
            val practitionerWorkload = practitionerWorkloads[teamStaffId]
              ?: getTeamOverviewForOffenderManagerWithoutWorkload(it.code, it.retrieveGrade()!!, team.key)
            Practitioner.from(it, practitionerWorkload, practitionerCaseCounts.getOrDefault(teamStaffId, 0))
          }
      }
    }
  }
}
