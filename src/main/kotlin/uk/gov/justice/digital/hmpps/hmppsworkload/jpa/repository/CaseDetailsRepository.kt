package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity

interface CaseDetailsRepository : CrudRepository<CaseDetailsEntity, String> {

  @Suppress("LongParameterList")
  @Modifying
  @Query(
    value = """INSERT INTO case_details(first_name, surname, tier, "type", crn) VALUES (:firstName, :surname, :tier, :caseType, :crn)
     ON CONFLICT (crn) DO UPDATE  set first_name = excluded.first_name, surname = excluded.surname, tier = excluded.tier, type = excluded.type, crn = excluded.crn;""",
    nativeQuery = true,
  )
  fun insertCaseDetails(
    @Param("firstName") firstName: String,
    @Param("surname") surname: String,
    @Param("tier") tier: String,
    @Param("caseType") caseType: String,
    @Param("crn") crn: String,
  )
}
