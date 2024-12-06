package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.CompleteDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseCount
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CreatedAllocationDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.EventDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.EventManagerDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.EventManagerAuditRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.EventManagerRepository
import java.time.ZonedDateTime
import java.util.UUID

@Service
class JpaBasedGetEventManager(
  private val eventManagerRepository: EventManagerRepository,
  private val caseDetailsRepository: CaseDetailsRepository,
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  private val eventManagerAuditRepository: EventManagerAuditRepository,
) {
  fun findById(id: UUID): EventManagerDetails? = eventManagerRepository.findByUuid(id)?.let { eventManagerEntity ->
    val eventManagerAudit = eventManagerAuditRepository.findFirstByEventManagerOrderByCreatedDateDesc(eventManagerEntity)
    EventManagerDetails.from(eventManagerEntity, eventManagerAudit)
  }
  fun findLatestByStaffAndTeam(staffIdentifier: StaffIdentifier): EventDetails? =
    eventManagerRepository.findFirstByStaffCodeAndTeamCodeAndIsActiveTrueOrderByCreatedDateDesc(staffIdentifier.staffCode, staffIdentifier.teamCode)?.let { eventManagerEntity ->
      caseDetailsRepository.findByIdOrNull(eventManagerEntity.crn)?.let { caseDetails ->
        EventDetails(caseDetails.tier, caseDetails.type, caseDetails.crn, eventManagerEntity.createdDate!!)
      }
    }

  fun findDetailsByCrnAndEventNumber(crn: String, eventNumber: Int): CaseDetails? = eventManagerRepository.findFirstByCrnAndEventNumberOrderByCreatedDateDesc(crn, eventNumber)?.let { eventManagerEntity ->
    caseDetailsRepository.findByIdOrNull(eventManagerEntity.crn)?.let { caseDetailsEntity ->
      CaseDetails.from(caseDetailsEntity)
    }
  }

  suspend fun findCompleteDetailsByCrnAndEventNumber(crn: String, eventNumber: Int): CompleteDetails? = eventManagerRepository.findFirstByCrnAndEventNumberOrderByCreatedDateDesc(crn, eventNumber)?.let { eventManagerEntity ->
    workforceAllocationsToDeliusApiClient.allocationCompleteDetails(crn, eventNumber.toString(), eventManagerEntity.staffCode)
  }

  suspend fun findAllocationsByTeam(since: ZonedDateTime, teams: List<String>): CreatedAllocationDetails {
    val allocatedEventManagers = eventManagerRepository.findByCreatedDateGreaterThanEqualAndTeamCodeInAndIsActiveTrue(since, teams)
    val allocatedEventManagerDetails = allocatedEventManagers.takeUnless { it.isEmpty() }?.let { workforceAllocationsToDeliusApiClient.allocationDetails(allocatedEventManagers).cases.associateBy { it.crn } } ?: emptyMap()
    val caseDetails = caseDetailsRepository.findAllById(allocatedEventManagers.map { it.crn }).associateBy { it.crn }
    return CreatedAllocationDetails.from(allocatedEventManagers, allocatedEventManagerDetails, caseDetails)
  }

  suspend fun countAllocationsBy(since: ZonedDateTime, teams: List<String>): CaseCount {
    val allocatedEventManagers = eventManagerRepository.findByCreatedDateGreaterThanEqualAndTeamCodeInAndIsActiveTrue(since, teams)
    val caseDetails = caseDetailsRepository.findAllById(allocatedEventManagers.map { it.crn })
    return CaseCount(caseDetails.count())
  }
}
