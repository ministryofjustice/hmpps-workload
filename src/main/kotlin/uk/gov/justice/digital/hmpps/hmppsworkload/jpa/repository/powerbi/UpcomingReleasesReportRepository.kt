package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.UpcomingReleasesReportEntity

@Repository
interface UpcomingReleasesReportRepository : CrudRepository<UpcomingReleasesReportEntity, Long>
