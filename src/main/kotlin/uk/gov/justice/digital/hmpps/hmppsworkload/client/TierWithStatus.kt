package uk.gov.justice.digital.hmpps.hmppsworkload.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class TierWithStatus @JsonCreator constructor(
  @JsonProperty("tierScore")
  val tierScore: String,
  @JsonProperty("provisional")
  val provisional: Boolean,
)
