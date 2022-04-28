package uk.gov.justice.digital.hmpps.hmppsworkload.client.dto

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigInteger
import java.time.LocalDate

data class OffenderAssessment @JsonCreator constructor(
  val ogrsLastUpdate: LocalDate?,
  val ogrsScore: BigInteger?
)
