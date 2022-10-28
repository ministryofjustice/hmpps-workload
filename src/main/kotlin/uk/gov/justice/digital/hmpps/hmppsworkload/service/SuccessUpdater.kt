package uk.gov.justice.digital.hmpps.hmppsworkload.service

import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.ReductionEntity
import java.math.BigDecimal
import java.math.BigInteger
import java.time.ZonedDateTime
import java.util.UUID

interface SuccessUpdater {

  fun updatePerson(crn: String, allocationId: UUID, timeUpdated: ZonedDateTime)
  fun updateEvent(crn: String, allocationId: UUID, timeUpdated: ZonedDateTime)
  fun updateRequirement(crn: String, allocationId: UUID, timeUpdated: ZonedDateTime)
  fun outOfDateReductionsProcessed()
  fun auditAllocation(crn: String, eventId: BigInteger, loggedInUser: String, requirementIds: List<BigInteger>)

  fun auditReduction(reductionEntity: ReductionEntity, reductionStatus: String)

  fun staffAvailableHoursChange(staffCode: String, teamCode: String, availableHours: BigDecimal)
}
