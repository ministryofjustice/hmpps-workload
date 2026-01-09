package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.util.UUID

data class CaseReallocated @JsonCreator constructor(
  val personManagerId: UUID,
  val eventManagerId: List<UUID>,
  val requirementManagerIds: List<UUID>,
)
