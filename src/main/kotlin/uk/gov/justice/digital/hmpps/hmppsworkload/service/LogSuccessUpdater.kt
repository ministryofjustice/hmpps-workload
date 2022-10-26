package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.config.NoAllocationCompleteTopicCondition
import java.math.BigDecimal
import java.math.BigInteger
import java.time.ZonedDateTime
import java.util.UUID

@Service
@Conditional(NoAllocationCompleteTopicCondition::class)
class LogSuccessUpdater : SuccessUpdater {

  override fun updatePerson(crn: String, allocationId: UUID, timeUpdated: ZonedDateTime) {
    logUpdate(crn, allocationId, timeUpdated)
  }

  override fun updateEvent(crn: String, allocationId: UUID, timeUpdated: ZonedDateTime) {
    logUpdate(crn, allocationId, timeUpdated)
  }

  override fun updateRequirement(crn: String, allocationId: UUID, timeUpdated: ZonedDateTime) {
    logUpdate(crn, allocationId, timeUpdated)
  }

  override fun outOfDateReductionsProcessed() {
    log.info("out of date reductions processed")
  }

  override fun auditAllocation(
    crn: String,
    eventId: BigInteger,
    loggedInUser: String,
    requirementIds: List<BigInteger>
  ) {
    log.info("allocation audited")
  }

  override fun staffAvailableHoursChange(staffCode: String, teamCode: String, availableHours: BigDecimal) {
    SqsSuccessPublisher.log.info("staff available hours changed message for {} in team {} with hours {}", staffCode, teamCode, availableHours)
  }

  private fun logUpdate(crn: String, allocationId: UUID, timeUpdated: ZonedDateTime) {
    log.info("crn {} with allocationId {} updated at {}", crn, allocationId, timeUpdated)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
