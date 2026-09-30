package uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi

data class ReportPractitionerData(
  val ispsDueInNext14Days: Map<ReportPractitionerId, Int>,
  val contactSuspendedCases: Map<ReportPractitionerId, Int>,
  val custodyReleasesInNext7Days: Map<ReportPractitionerId, Int>,
  val paroleReportsInNext28Days: Map<ReportPractitionerId, Int>,
  val hdcrotlReportsInNext14Days: Map<ReportPractitionerId, Int>,
  val partBReportsInNext14Days: Map<ReportPractitionerId, Int>,
  val partCReportsInNext14Days: Map<ReportPractitionerId, Int>,
)
