package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ImpactResponse
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Name
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.OverviewOffenderManager
import java.math.BigDecimal

data class OffenderManagerPotentialWorkload @JsonCreator constructor(
  @Schema(description = "Probation Practitioner capacity correct to one decimal place", example = "56.7")
  val capacity: BigDecimal,
  @Schema(description = "Probation Practitioner potential capacity after case is allocated", example = "8.2")
  val potentialCapacity: BigDecimal,
  @Schema(description = "Name of Person on Probation")
  val name: Name,
  @Schema(description = "Name of staff member")
  val staff: StaffMember,
  val tier: Tier,
  val provisionalTier: Boolean,
) {
  companion object {
    fun from(offenderManagerOverview: OverviewOffenderManager, impactResponse: ImpactResponse, potentialCase: Case): OffenderManagerPotentialWorkload = OffenderManagerPotentialWorkload(
      offenderManagerOverview.capacity,
      offenderManagerOverview.potentialCapacity!!,
      impactResponse.name,
      impactResponse.staff,
      potentialCase.tier,
      potentialCase.provisionalTier,
    )
  }
}
