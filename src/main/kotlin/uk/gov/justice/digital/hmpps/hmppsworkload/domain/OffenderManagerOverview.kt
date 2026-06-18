package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.OfficerView
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.OverviewOffenderManager
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZonedDateTime

data class OffenderManagerOverview @JsonCreator constructor(
  @Schema(description = "Probation Practitioner forename", example = "John")
  val forename: String,
  @Schema(description = "Probation Practitioner surname", example = "Smith")
  val surname: String,
  @Schema(description = "Probation Practitioner Grade", example = "PO")
  val grade: String,
  @Schema(description = "Probation Practitioner capacity as a decimal", example = "0.5")
  val capacity: BigDecimal,
  @Schema(description = "Offender Manager Code", example = "OM1")
  val code: String,
  @Schema(description = "Offender Manager e-mail address", example = "example.user@test.justice.gov.uk")
  val email: String?,
  @Schema(description = "Probation Practitioner total cases", example = "25")
  val totalCases: Long,
  @Schema(description = "Probation Practitioner Contracted hours per week", example = "37")
  val weeklyHours: BigDecimal,
  @Schema(description = "Probation Practitioner total Reduction hours per week", example = "10")
  val totalReductionHours: BigDecimal,
  @Schema(description = "Probation Practitioner total Points available", example = "2176")
  val pointsAvailable: BigInteger,
  @Schema(description = "Probation Practitioner total Points used", example = "1567")
  val pointsUsed: BigInteger,
  @Schema(description = "Probation Practitioner total Points remaining", example = "609")
  val pointsRemaining: BigInteger,
  @Schema(description = "Last time the Capacity was updated", example = "2013-11-03T09:00:00")
  val lastUpdatedOn: LocalDateTime?,
  @Schema(description = "Next time the reduction total will change", example = "2013-11-03T09:00:00")
  val nextReductionChange: ZonedDateTime?,
  val caseTotals: TierCaseTotals,
  @Schema(description = "Probation Practitioner parole reports due in the next 30 days", example = "5")
  val paroleReportsDue: BigInteger,
  @Schema(description = "Probation Practitioner cases due to end in the next 30 days", example = "3")
  val caseEndDue: BigInteger,
  @Schema(description = "Probation Practitioner cases due to be released in the next 30 days", example = "6")
  val releasesDue: BigInteger,
  val lastAllocatedEvent: LastAllocatedEvent?,
) {
  companion object {
    fun from(overviewOffenderManager: OverviewOffenderManager, officerView: OfficerView): OffenderManagerOverview = OffenderManagerOverview(
      officerView.name.forename,
      officerView.name.surname,
      officerView.getGrade(),
      overviewOffenderManager.capacity,
      overviewOffenderManager.code,
      officerView.email,
      overviewOffenderManager.totalCommunityCases.plus(overviewOffenderManager.totalLicenseCases).plus(overviewOffenderManager.totalCustodyCases),
      overviewOffenderManager.contractedHours,
      overviewOffenderManager.reductionHours,
      overviewOffenderManager.availablePoints,
      overviewOffenderManager.totalPoints,
      overviewOffenderManager.availablePoints.minus(overviewOffenderManager.totalPoints),
      overviewOffenderManager.lastUpdatedOn,
      overviewOffenderManager.nextReductionChange,
      overviewOffenderManager.tierCaseTotals,
      overviewOffenderManager.paroleReportsDue,
      officerView.casesDueToEndInNext4Weeks,
      officerView.releasesWithinNext4Weeks,
      overviewOffenderManager.lastAllocatedEvent?.let { LastAllocatedEvent.from(it) },
    )
  }
}

data class LastAllocatedEvent @JsonCreator constructor(
  val allocatedOn: ZonedDateTime,
  val tier: Tier,
  val sentenceType: CaseType,
) {
  companion object {
    fun from(eventDetails: EventDetails): LastAllocatedEvent = LastAllocatedEvent(eventDetails.allocatedOn, eventDetails.tier, eventDetails.type)
  }
}
