package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping

data class OffenderManagerCase(
  val crn: String,
  val tier: String,
  val caseCategory: String
)
