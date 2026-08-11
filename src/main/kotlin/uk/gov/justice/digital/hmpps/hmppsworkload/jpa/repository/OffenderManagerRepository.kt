package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.OffenderManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.OverviewOffenderManager

@Repository
interface OffenderManagerRepository : CrudRepository<OffenderManagerEntity, Long> {
  @Query(nativeQuery = true)
  fun findByOverview(teamCode: String, offenderManagerCode: String): OverviewOffenderManager?

  @Query(nativeQuery = true)
  fun findCasesByTeamCodeAndStaffCode(offenderManagerCode: String, teamCode: String): List<String>

  @Query(nativeQuery = true)
  fun findCaseByTeamCodeAndStaffCodeAndCrn(teamCode: String, offenderManagerCode: String, crn: String): String?

  fun findByCode(staffCode: String): OffenderManagerEntity?
}
