package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator

data class Case @JsonCreator constructor(
  val tier: Tier,
  val provisionalTier: Boolean,
  val type: CaseType,
  val isT2A: Boolean = false,
  val crn: String,
)
