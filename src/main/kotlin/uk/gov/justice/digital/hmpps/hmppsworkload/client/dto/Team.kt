package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Team @JsonCreator constructor(
  @JsonProperty("code") val code: String,
  @JsonProperty("description") val description: String,
  @JsonProperty("localAdminUnit") val localAdminUnit: LocalAdminUnit
)

data class LocalAdminUnit @JsonCreator constructor(
  @JsonProperty("code") val code: String,
  @JsonProperty("description") val description: String,
  @JsonProperty("probationDeliveryUnit") val probationDeliveryUnit: ProbationDeliveryUnit
)

data class ProbationDeliveryUnit @JsonCreator constructor(
  @JsonProperty("code") val code: String,
  @JsonProperty("description") val description: String,
  @JsonProperty("provider") val provider: Provider
)

data class Provider @JsonCreator constructor(
  @JsonProperty("code") val code: String,
  @JsonProperty("description") val description: String
  
