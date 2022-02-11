package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.TeamEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.OffenderManagerOverview

@Repository
interface TeamRepository : CrudRepository<TeamEntity, Long> {
  @Query(nativeQuery = true)
  fun findByOverview(teamCode: String): List<OffenderManagerOverview>

  fun existsByCode(code: String): Boolean
}
