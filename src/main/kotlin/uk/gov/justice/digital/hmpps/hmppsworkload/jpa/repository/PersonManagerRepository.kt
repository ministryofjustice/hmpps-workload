package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import java.time.ZonedDateTime
import java.util.UUID

interface PersonManagerRepository : CrudRepository<PersonManagerEntity, Long> {
  fun findFirstByCrnOrderByCreatedDateDesc(crn: String): PersonManagerEntity?
  fun findByUuid(id: UUID): PersonManagerEntity?

  fun findByTeamCodeInAndCreatedDateGreaterThanEqualAndIsActiveIsTrue(teamCodes: List<String>, createdDate: ZonedDateTime): List<PersonManagerEntity>
  fun findByStaffCodeAndTeamCodeAndIsActiveIsTrue(staffCode: String, teamCode: String): List<PersonManagerEntity>
  fun findByTeamCodeInAndIsActiveIsTrue(teamCodes: List<String>): List<PersonManagerEntity>

  @Modifying
  @Query("update PersonManagerEntity p set p.isActive= false where p.crn = ?1")
  fun setInactiveTrueFor(crn: String): Int
}
