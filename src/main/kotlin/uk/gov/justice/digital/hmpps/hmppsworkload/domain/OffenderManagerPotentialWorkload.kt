package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ImpactResponse
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Name
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember

data class OffenderManagerPotentialWorkload @JsonCreator constructor(
  @Schema(description = "Name of Person on Probation")
  val name: Name,
  @Schema(description = "Name of staff member")
  val staff: StaffMember,
  val tier: Tier,
  val provisionalTier: Boolean,
) {
  companion object {
    fun from(impactResponse: ImpactResponse, potentialCase: Case): OffenderManagerPotentialWorkload = OffenderManagerPotentialWorkload(
      impactResponse.name,
      impactResponse.staff,
      potentialCase.tier,
      potentialCase.provisionalTier,
    )
  }
}
