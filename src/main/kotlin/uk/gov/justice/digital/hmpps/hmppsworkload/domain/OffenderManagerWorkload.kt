package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.TeamOverview
import java.math.BigDecimal
import java.math.BigInteger

data class OffenderManagerWorkload @JsonCreator constructor(
  @Schema(description = "Probation Practitioner forename", example = "John")
  val forename: String,
  @Schema(description = "Probation Practitioner surname", example = "Smith")
  val surname: String,
  @Schema(description = "Probation Practitioner Grade", example = "PO")
  val grade: String,
  @Schema(description = "Probation Practitioner Total current Community and Licence case count", example = "10")
  val totalCommunityCases: BigDecimal,
  @Schema(description = "Probation Practitioner Total custody case count", example = "5")
  val totalCustodyCases: BigDecimal,
  @Schema(description = "Probation Practitioner capacity as a decimal", example = "0.5")
  val capacity: BigDecimal,
  @Schema(description = "Offender Manager Code", example = "OM1")
  val code: String,
  @Schema(description = "StaffId", example = "1111111111")
  val staffId: BigInteger,
  @Schema(description = "Probation Practitioner Total cases allocated in the last week", example = "3")
  val totalCasesInLastWeek: BigInteger
) {
  companion object {
    fun from(teamOverview: TeamOverview): OffenderManagerWorkload {

      return OffenderManagerWorkload(
        teamOverview.forename,
        teamOverview.surname,
        teamOverview.grade,
        teamOverview.totalCommunityCases,
        teamOverview.totalCustodyCases,
        teamOverview.capacity,
        teamOverview.code,
        teamOverview.staffId,
        teamOverview.casesInLastWeek
      )
    }
  }
}
