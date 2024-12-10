package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocationDetail
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Name
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import java.time.ZonedDateTime

data class CreatedAllocationDetails(val cases: List<CreatedAllocationDetail>) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    fun from(eventManagers: List<EventManagerEntity>, eventManagerDetails: Map<String, AllocationDetail>, caseDetails: Map<String, CaseDetailsEntity>): CreatedAllocationDetails {
      return CreatedAllocationDetails(
        eventManagers
          .filter {
            val detailsExist = eventManagerDetails.containsKey(it.crn) && caseDetails.containsKey(it.crn)
            if (!detailsExist) {
              log.info("Retrieving allocated events crn {} delius details exist {}, case details exist {}", it.crn, eventManagerDetails.containsKey(it.crn), caseDetails.containsKey(it.crn))
            }
            detailsExist
          }
          .map { CreatedAllocationDetail.from(eventManagerDetails[it.crn]!!, caseDetails[it.crn]!!, it) },
      )
    }
  }
}

data class CreatedAllocationDetail(
  val crn: String,
  val name: Name,
  val staff: StaffMember,
  val tier: Tier,
  val allocatedOn: ZonedDateTime,
  val allocatingSpo: String,
  val teamCode: String,
) {
  companion object {
    fun from(allocationDetail: AllocationDetail, caseDetail: CaseDetailsEntity, eventManagerEntity: EventManagerEntity): CreatedAllocationDetail {
      return CreatedAllocationDetail(eventManagerEntity.crn, allocationDetail.name, allocationDetail.staff, caseDetail.tier, eventManagerEntity.createdDate!!, eventManagerEntity.spoName!!, eventManagerEntity.teamCode)
    }
  }
}
