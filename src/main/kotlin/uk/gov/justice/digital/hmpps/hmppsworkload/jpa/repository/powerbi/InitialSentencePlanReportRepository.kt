package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.InitialSentencePlanReportEntity
import java.time.LocalDate

@Repository
interface InitialSentencePlanReportRepository : CrudRepository<InitialSentencePlanReportEntity, Long> {
  fun findAllByTeamInAndTargetDateLessThanEqual(teams: List<String>, targetDate: LocalDate): List<InitialSentencePlanReportEntity>
}
