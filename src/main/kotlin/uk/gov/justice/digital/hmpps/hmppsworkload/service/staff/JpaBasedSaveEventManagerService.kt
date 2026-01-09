package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.ReallocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.SaveResult
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerAuditEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.EventManagerAuditRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.EventManagerRepository
import java.time.ZonedDateTime

private const val TOLERANCE_MINUTES = 5L

@Suppress("NestedBlockDepth")
@Service
class JpaBasedSaveEventManagerService(
  private val eventManagerRepository: EventManagerRepository,
  private val eventManagerAuditRepository: EventManagerAuditRepository,
) : SaveEventManagerService {

  @Suppress("NestedBlockDepth")
  @Transactional
  /***
   * if the case has an event manager check if the new event manager is the same otherwise make the older event manager
   * inactive and save the new event manager.
   */
  override fun saveEventManager(teamCode: String, deliusStaff: StaffMember, allocateCase: AllocateCase, loggedInUser: String, spoStaffCode: String, spoName: String): SaveResult<EventManagerEntity> = eventManagerRepository.findFirstByCrnAndEventNumberOrderByCreatedDateDesc(allocateCase.crn, allocateCase.eventNumber)?.let { eventManager ->
    var lastEventCreatedDate = eventManager.createdDate
    val timeNow = ZonedDateTime.now()

    if (eventManager.staffCode == deliusStaff.code && eventManager.teamCode == teamCode) {
      // reset createdDate if enough time has elapsed and therefore valid reallocation
      eventManager.createdDate?.let {
        if (it.isBefore(timeNow.minusMinutes(TOLERANCE_MINUTES))) {
          eventManager.createdDate = timeNow
        }
      }

      if (eventManager.createdDate!!.isEqual(lastEventCreatedDate)) {
        return SaveResult(eventManager, false)
      }
    }
    eventManager.isActive = false
    eventManagerRepository.save(eventManager)
    saveEventManagerEntity(allocateCase, deliusStaff, teamCode, loggedInUser, spoStaffCode, spoName)
  } ?: saveEventManagerEntity(allocateCase, deliusStaff, teamCode, loggedInUser, spoStaffCode, spoName)

  override fun saveEventManager(teamCode: String, deliusStaff: StaffMember, reallocateCase: ReallocateCase, loggedInUser: String, spoStaffCode: String, spoName: String, eventNumber: Int): SaveResult<EventManagerEntity> {
    var allocatedCase = AllocateCase(
      reallocateCase.crn, "", null, false,
      eventNumber, null, null, reallocateCase.reallocationNotes, reallocateCase.sensitiveNotes,
      reallocateCase.laoCase,
    )

    return saveEventManager(teamCode, deliusStaff, allocatedCase, loggedInUser, spoStaffCode, spoName)
  }

  @Suppress("LongParameterList")
  private fun saveEventManagerEntity(
    allocateCase: AllocateCase,
    deliusStaff: StaffMember,
    teamCode: String,
    loggedInUser: String,
    spoStaffCode: String?,
    spoName: String?,
  ): SaveResult<EventManagerEntity> {
    val eventManagerEntity = EventManagerEntity(
      crn = allocateCase.crn,
      staffCode = deliusStaff.code,
      teamCode = teamCode,
      createdBy = loggedInUser,
      isActive = true,
      eventNumber = allocateCase.eventNumber,
      spoStaffCode = spoStaffCode,
      spoName = spoName,
    )
    eventManagerRepository.save(eventManagerEntity)
    auditEventManagerAllocation(allocateCase, loggedInUser, eventManagerEntity)
    return SaveResult(eventManagerEntity, true)
  }

  private fun auditEventManagerAllocation(allocateCase: AllocateCase, loggedInUser: String, eventManagerEntity: EventManagerEntity) {
    eventManagerAuditRepository.save(
      EventManagerAuditEntity(
        spoOversightNotes = allocateCase.spoOversightNotes,
        sensitiveOversightNotes = allocateCase.sensitiveOversightNotes,
        allocationJustificationNotes = allocateCase.allocationJustificationNotes,
        sensitiveNotes = allocateCase.sensitiveNotes,
        createdBy = loggedInUser,
        eventManager = eventManagerEntity,
      ),
    )
  }
}
