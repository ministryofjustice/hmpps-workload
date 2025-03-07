package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import java.time.ZonedDateTime
import java.util.UUID

data class PersonManagerDetails @JsonCreator constructor(
  @Schema(description = "Probation Practitioner ID")
  val id: UUID,
  @Schema(description = "Staff Code")
  val staffCode: String,
  @Schema(description = "Team Code")
  val teamCode: String,
  @Schema(description = "Created date")
  var createdDate: ZonedDateTime,
  @Schema(description = "crn")
  val crn: String,
) {
  companion object {
    fun from(personManagerEntity: PersonManagerEntity): PersonManagerDetails = PersonManagerDetails(
      personManagerEntity.uuid,
      personManagerEntity.staffCode,
      personManagerEntity.teamCode,
      personManagerEntity.createdDate!!,
      personManagerEntity.crn,
    )
  }
}
