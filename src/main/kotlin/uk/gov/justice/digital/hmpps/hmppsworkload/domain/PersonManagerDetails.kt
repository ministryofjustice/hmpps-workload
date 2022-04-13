package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import java.math.BigInteger
import java.time.ZonedDateTime
import java.util.UUID

data class PersonManagerDetails @JsonCreator constructor(
  @Schema(description = "Probation Practitioner ID")
  val id: UUID,
  @Schema(description = "Staff Identifier")
  val staffId: BigInteger,
  @Schema(description = "Staff Code")
  val staffCode: String,
  @Schema(description = "Team Code")
  val teamCode: String,
  @Schema(description = "Provider Code")
  val providerCode: String,
  @Schema(description = "Username of who created")
  val createdBy: String,
  @Schema(description = "Created date")
  val createdDate: ZonedDateTime
) {
  companion object {
    fun from(personManagerEntity: PersonManagerEntity): PersonManagerDetails {
      return PersonManagerDetails(personManagerEntity.uuid, personManagerEntity.staffId, personManagerEntity.staffCode, personManagerEntity.teamCode, personManagerEntity.providerCode, personManagerEntity.createdBy, personManagerEntity.createdDate!!)
    }
  }
}
