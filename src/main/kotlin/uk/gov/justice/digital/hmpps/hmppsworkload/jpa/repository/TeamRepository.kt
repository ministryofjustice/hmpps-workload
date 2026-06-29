package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.TeamEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.TeamOverview
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.WorkloadCaseResult

@Repository
interface TeamRepository : CrudRepository<TeamEntity, Long> {

  @Query(nativeQuery = true)
  fun findAllByTeamCodes(teamCodes: List<String>): List<TeamOverview>

  fun findAllByCodeIn(codes: List<String>): List<TeamEntity>

  fun findByCode(code: String): TeamEntity?

  @Query(nativeQuery = true)
  fun findWorkloadCountCaseByCode(code: List<String>): List<WorkloadCaseResult>
}
