package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerAuditEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import java.time.ZonedDateTime
import java.util.UUID

data class EventManagerDetails @JsonCreator constructor(
  @Schema(description = "Probation Practitioner ID")
  val id: UUID,
  @Schema(description = "Staff Code")
  val staffCode: String,
  @Schema(description = "Team Code")
  val teamCode: String,
  @Schema(description = "Created date")
  var createdDate: ZonedDateTime,
  @Schema(description = "event Number")
  val eventNumber: Int?,
  @Schema(description = "SPO Oversight Notes")
  val spoOversightNotes: String?,
  @Schema(description = "SPO Oversight notes contain sensitive information")
  val sensitiveOversightNotes: Boolean?,
  @Schema(description = "Allocation Justification Notes")
  val allocationJustificationNotes: String?,
  @Schema(description = "Justification notes contain sensitive information")
  val sensitiveNotes: Boolean?,
  @Schema(description = "SPO Staff Code")
  val spoStaffCode: String?,
  @Schema(description = "SPO Name")
  val spoName: String?,
) {
  companion object {
    fun from(eventManagerEntity: EventManagerEntity, eventManagerAuditEntity: EventManagerAuditEntity?): EventManagerDetails = EventManagerDetails(eventManagerEntity.uuid, eventManagerEntity.staffCode, eventManagerEntity.teamCode, eventManagerEntity.createdDate!!, eventManagerEntity.eventNumber, eventManagerAuditEntity?.spoOversightNotes, eventManagerAuditEntity?.sensitiveOversightNotes, eventManagerAuditEntity?.allocationJustificationNotes, eventManagerAuditEntity?.sensitiveNotes, eventManagerEntity.spoStaffCode, eventManagerEntity.spoName)
  }
}
