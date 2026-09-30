package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity

data class CaseDetails @JsonCreator constructor(
  @Schema(description = "Tier")
  val tier: Tier,
  @Schema(description = "Whether tier score is provisional")
  val provisionalTier: Boolean,
  @Schema(description = "Person on probation first name")
  val personOnProbationFirstName: String,
  @Schema(description = "Person on probation surname")
  val personOnProbationSurname: String,
) {
  companion object {
    fun from(caseDetailsEntity: CaseDetailsEntity): CaseDetails = CaseDetails(caseDetailsEntity.tier, caseDetailsEntity.provisionalTier, caseDetailsEntity.firstName, caseDetailsEntity.surname)
  }
}
