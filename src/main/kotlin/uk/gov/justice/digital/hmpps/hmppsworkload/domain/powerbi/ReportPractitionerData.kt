package uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi

data class ReportPractitionerData(
  val ispsDueInNext14Days: Map<ReportPractitionerId, Int>,
  val contactSuspendedCases: Map<ReportPractitionerId, Int>,
)
