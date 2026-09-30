package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import java.time.ZonedDateTime

data class EventDetails constructor(
  val tier: Tier,
  val provisionalTier: Boolean,
  val type: CaseType,
  val crn: String,
  val allocatedOn: ZonedDateTime,
)
