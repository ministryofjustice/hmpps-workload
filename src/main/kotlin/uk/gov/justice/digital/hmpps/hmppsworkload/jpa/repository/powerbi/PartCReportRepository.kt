package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.PartCReportEntity
import java.time.LocalDate

@Repository
interface PartCReportRepository : CrudRepository<PartCReportEntity, Long> {
  fun findAllByTeamInAndTargetDateLessThanEqual(teams: List<String>, targetDate: LocalDate): List<PartCReportEntity>
}
