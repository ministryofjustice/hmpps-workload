package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.OffenderManagerEntity

@Repository
interface OffenderManagerRepository : CrudRepository<OffenderManagerEntity, Long> {
  @Query(nativeQuery = true)
  fun findCasesByTeamCodeAndStaffCode(offenderManagerCode: String, teamCode: String): List<String>

  fun findByCode(staffCode: String): OffenderManagerEntity?
}
