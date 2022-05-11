package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator

data class Case @JsonCreator constructor(
  val tier: Tier,
  val type: CaseType,
  val isT2A: Boolean,
  val crn: String
)