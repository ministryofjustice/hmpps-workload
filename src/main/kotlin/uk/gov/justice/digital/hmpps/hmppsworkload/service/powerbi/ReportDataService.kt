package uk.gov.justice.digital.hmpps.hmppsworkload.service.powerbi

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.ReportPractitionerData
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.ReportPractitionerId
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi.InitialSentencePlanReportRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi.ResetReportRepository
import java.time.LocalDate

@Service
class ReportDataService(
  private val initialSentencePlanReportRepository: InitialSentencePlanReportRepository,
  private val resetReportRepository: ResetReportRepository,
) {
  fun getPractitionerData(teamNames: List<String>): ReportPractitionerData = ReportPractitionerData(
    getIspsDueInNext14Days(teamNames),
    getContactSuspendedCases(teamNames),
  )

  private fun getIspsDueInNext14Days(teamNames: List<String>): Map<ReportPractitionerId, Int> = initialSentencePlanReportRepository.findAllByTeamInAndTargetDateLessThanEqual(teamNames, LocalDate.now().plusDays(14))
    .groupBy { ReportPractitionerId(it.team, it.probationPractitioner) }
    .mapValues { entry -> entry.value.size }

  private fun getContactSuspendedCases(teamNames: List<String>): Map<ReportPractitionerId, Int> = resetReportRepository.findAllByTeamIn(teamNames)
    .groupBy { ReportPractitionerId(it.team, it.probationPractitioner) }
    .mapValues { entry -> entry.value.size }
}
