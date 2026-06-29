package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.InitialSentencePlanReportEntity
import java.time.LocalDate

@Repository
interface InitialSentencePlanReportRepository : CrudRepository<InitialSentencePlanReportEntity, Long> {
  // TODO: Is including overdue ISPs expected?
  fun findAllByTeamInAndTargetDateLessThanEqual(teams: List<String>, targetDate: LocalDate): List<InitialSentencePlanReportEntity>
}
