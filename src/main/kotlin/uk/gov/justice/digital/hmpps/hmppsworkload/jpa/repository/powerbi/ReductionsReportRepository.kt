package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.powerbi

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi.ReductionsReportEntity

@Repository
interface ReductionsReportRepository : CrudRepository<ReductionsReportEntity, Long> {
  fun findAllByStaffId(staffId: String): List<ReductionsReportEntity>
}
