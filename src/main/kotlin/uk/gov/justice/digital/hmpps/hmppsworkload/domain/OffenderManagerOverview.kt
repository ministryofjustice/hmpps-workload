package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.OfficerView
import java.math.BigInteger
import java.time.ZonedDateTime

data class OffenderManagerOverview @JsonCreator constructor(
  @Schema(description = "Probation Practitioner forename", example = "John")
  val forename: String,
  @Schema(description = "Probation Practitioner surname", example = "Smith")
  val surname: String,
  @Schema(description = "Probation Practitioner Grade", example = "PO")
  val grade: String,
  @Schema(description = "Offender Manager Code", example = "OM1")
  val code: String,
  @Schema(description = "Offender Manager e-mail address", example = "example.user@test.justice.gov.uk")
  val email: String?,
  @Schema(description = "Probation Practitioner total cases", example = "25")
  val totalCases: Long,
  val caseTotals: TierCaseTotals,
  @Schema(description = "Probation Practitioner cases due to end in the next 30 days", example = "3")
  val caseEndDue: BigInteger,
  @Schema(description = "Probation Practitioner cases due to be released in the next 30 days", example = "6")
  val releasesDue: BigInteger,
  val lastAllocatedEvent: LastAllocatedEvent?,
) {
  companion object {
    fun from(offenderManagerCases: OffenderManagerCases, tierCaseTotals: TierCaseTotals, lastAllocatedEvent: EventDetails?, officerView: OfficerView): OffenderManagerOverview = OffenderManagerOverview(
      officerView.name.forename,
      officerView.name.surname,
      officerView.getGrade(),
      officerView.code,
      officerView.email,
      offenderManagerCases.activeCases.size.toLong(),
      tierCaseTotals,
      officerView.casesDueToEndInNext4Weeks,
      officerView.releasesWithinNext4Weeks,
      lastAllocatedEvent?.let { LastAllocatedEvent.from(it) },
    )
  }
}

data class LastAllocatedEvent @JsonCreator constructor(
  val allocatedOn: ZonedDateTime,
  val tier: Tier,
  val provisionalTier: Boolean,
  val sentenceType: CaseType,
) {
  companion object {
    fun from(eventDetails: EventDetails): LastAllocatedEvent = LastAllocatedEvent(eventDetails.allocatedOn, eventDetails.tier, eventDetails.provisionalTier, eventDetails.type)
  }
}
