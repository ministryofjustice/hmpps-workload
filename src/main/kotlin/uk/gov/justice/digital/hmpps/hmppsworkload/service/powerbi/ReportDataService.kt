package uk.gov.justice.digital.hmpps.hmppsworkload.service.powerbi

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.ReportPractitionerData
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.ReportPractitionerId
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi.HDCROTLReportRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi.InitialSentencePlanReportRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi.ParoleReportRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi.PartBReportRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi.PartCReportRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi.ResetReportRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi.UpcomingReleasesReportRepository
import java.time.LocalDate

@Service
class ReportDataService(
  private val initialSentencePlanReportRepository: InitialSentencePlanReportRepository,
  private val resetReportRepository: ResetReportRepository,
  private val upcomingReleasesReportRepository: UpcomingReleasesReportRepository,
  private val paroleReportRepository: ParoleReportRepository,
  private val hdcrotlReportRepository: HDCROTLReportRepository,
  private val partBReportRepository: PartBReportRepository,
  private val partCReportRepository: PartCReportRepository,
) {
  fun getPractitionerData(teamNames: List<String>): ReportPractitionerData = ReportPractitionerData(
    getIspsDueInNext14Days(teamNames),
    getContactSuspendedCases(teamNames),
    getCustodyReleasesInNext7Days(teamNames),
    getParoleReportsInNext28Days(teamNames),
    getHdcRotlReportsInNext14Days(teamNames),
    getPartBReportsInNext14Days(teamNames),
    getPartCReportsInNext14Days(teamNames),
  )

  private fun getIspsDueInNext14Days(teamNames: List<String>): Map<ReportPractitionerId, Int> = initialSentencePlanReportRepository.findAllByTeamInAndTargetDateLessThanEqual(teamNames, LocalDate.now().plusDays(14))
    .groupBy { ReportPractitionerId(it.team, it.probationPractitioner) }
    .mapValues { entry -> entry.value.size }

  private fun getContactSuspendedCases(teamNames: List<String>): Map<ReportPractitionerId, Int> = resetReportRepository.findAllByTeamIn(teamNames)
    .groupBy { ReportPractitionerId(it.team, it.probationPractitioner) }
    .mapValues { entry -> entry.value.size }

  private fun getCustodyReleasesInNext7Days(teamNames: List<String>): Map<ReportPractitionerId, Int> = upcomingReleasesReportRepository.findAllByTeamInAndExpectedReleaseDateLessThanEqual(teamNames, LocalDate.now().plusDays(7))
    .groupBy { ReportPractitionerId(it.team, it.probationPractitioner) }
    .mapValues { entry -> entry.value.size }

  private fun getParoleReportsInNext28Days(teamNames: List<String>): Map<ReportPractitionerId, Int> = paroleReportRepository.findAllByTeamInAndTargetDateLessThanEqual(teamNames, LocalDate.now().plusDays(28))
    .groupBy { ReportPractitionerId(it.team, it.probationPractitioner) }
    .mapValues { entry -> entry.value.size }

  private fun getHdcRotlReportsInNext14Days(teamNames: List<String>): Map<ReportPractitionerId, Int> = hdcrotlReportRepository.findAllByTeamInAndTargetDateLessThanEqual(teamNames, LocalDate.now().plusDays(14))
    .groupBy { ReportPractitionerId(it.team, it.probationPractitioner) }
    .mapValues { entry -> entry.value.size }

  private fun getPartBReportsInNext14Days(teamNames: List<String>): Map<ReportPractitionerId, Int> = partBReportRepository.findAllByTeamInAndTargetDateLessThanEqual(teamNames, LocalDate.now().plusDays(14))
    .groupBy { ReportPractitionerId(it.team, it.probationPractitioner) }
    .mapValues { entry -> entry.value.size }

  private fun getPartCReportsInNext14Days(teamNames: List<String>): Map<ReportPractitionerId, Int> = partCReportRepository.findAllByTeamInAndTargetDateLessThanEqual(teamNames, LocalDate.now().plusDays(14))
    .groupBy { ReportPractitionerId(it.team, it.probationPractitioner) }
    .mapValues { entry -> entry.value.size }
}
