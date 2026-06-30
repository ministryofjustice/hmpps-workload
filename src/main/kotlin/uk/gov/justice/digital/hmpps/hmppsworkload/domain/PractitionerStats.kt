package uk.gov.justice.digital.hmpps.hmppsworkload.domain

data class PractitionerStats(
  val allocatedCaseCount: Int,
  val reallocatedCaseCount: Int,
  val ispsDueInNext14Days: Int,
  val contactSuspendedCases: Int,
  val custodyReleasesInNext7Days: Int,
  val tierCaseTotals: TierCaseTotals?,
)
