package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.UpcomingReleasesReportEntity
import java.time.LocalDate

@Repository
interface UpcomingReleasesReportRepository : CrudRepository<UpcomingReleasesReportEntity, Long> {
  fun findAllByTeamInAndExpectedReleaseDateLessThanEqual(teams: List<String>, expectedReleaseDate: LocalDate): List<UpcomingReleasesReportEntity>
}
