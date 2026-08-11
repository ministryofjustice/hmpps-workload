package uk.gov.justice.digital.hmpps.hmppsworkload.domain

data class UpdatedCaseDetails(
  val firstName: String,
  val surname: String,
  val tier: Tier,
  val provisionalTier: Boolean,
  val caseType: CaseType,
  val crn: String,
)
