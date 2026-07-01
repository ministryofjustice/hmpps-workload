package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.PartBReportEntity
import java.time.LocalDate

@Repository
interface PartBReportRepository : CrudRepository<PartBReportEntity, Long> {
  fun findAllByTeamInAndTargetDateLessThanEqual(teams: List<String>, targetDate: LocalDate): List<PartBReportEntity>
}
