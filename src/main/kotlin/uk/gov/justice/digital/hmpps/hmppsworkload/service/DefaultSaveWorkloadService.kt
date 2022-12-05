package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.DeliusStaff
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseAllocated
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.SaveResult
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.RequirementManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.SaveEventManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.SavePersonManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.SaveRequirementManagerService

@Service
class DefaultSaveWorkloadService(
  private val savePersonManagerService: SavePersonManagerService,
  private val communityApiClient: CommunityApiClient,
  private val saveEventManagerService: SaveEventManagerService,
  private val saveRequirementManagerService: SaveRequirementManagerService,
  private val notificationService: NotificationService,
  private val telemetryService: TelemetryService,
  private val sqsSuccessPublisher: SqsSuccessPublisher,
  private val caseDetailsRepository: CaseDetailsRepository
) {

  fun saveWorkload(
    allocatedStaffId: StaffIdentifier,
    allocateCase: AllocateCase,
    loggedInUser: String,
    authToken: String
  ): CaseAllocated {
    val allocateTo = communityApiClient.getStaffByCode(allocatedStaffId.staffCode).block()!!
    val activeRequirements = communityApiClient.getActiveRequirements(allocateCase.crn, allocateCase.eventId).block()!!.requirements
    val personManagerSaveResult = savePersonManagerService.savePersonManager(
      allocatedStaffId.teamCode,
      allocateTo,
      loggedInUser,
      allocateCase.crn
    ).also { afterPersonManagerSaved(it, allocateTo) }
    val eventManagerSaveResult = saveEventManagerService.saveEventManager(allocatedStaffId.teamCode, allocateTo, allocateCase, loggedInUser).also { afterEventManagerSaved(it) }
    val requirementManagerSaveResults = saveRequirementManagerService.saveRequirementManagers(allocatedStaffId.teamCode, allocateTo, allocateCase, loggedInUser, activeRequirements).also { afterRequirementManagersSaved(it) }
    if (personManagerSaveResult.hasChanged || eventManagerSaveResult.hasChanged || requirementManagerSaveResults.any { it.hasChanged }) {
      notificationService.notifyAllocation(allocateTo, activeRequirements, allocateCase, loggedInUser, authToken)
        .block()
      sqsSuccessPublisher.auditAllocation(allocateCase.crn, allocateCase.eventNumber, loggedInUser, activeRequirements.map { it.requirementId })
    }
    return CaseAllocated(personManagerSaveResult.entity.uuid, eventManagerSaveResult.entity.uuid, requirementManagerSaveResults.map { it.entity.uuid })
  }

  private fun afterPersonManagerSaved(personManagerSaveResult: SaveResult<PersonManagerEntity>, deliusStaff: DeliusStaff) {
    if (personManagerSaveResult.hasChanged) {
      telemetryService.trackPersonManagerAllocated(personManagerSaveResult.entity)
      val caseDetails = caseDetailsRepository.findByIdOrNull(personManagerSaveResult.entity.crn)
      telemetryService.trackStaffGradeToTierAllocated(caseDetails, deliusStaff, personManagerSaveResult.entity.teamCode)
      sqsSuccessPublisher.updatePerson(
        personManagerSaveResult.entity.crn,
        personManagerSaveResult.entity.uuid,
        personManagerSaveResult.entity.createdDate!!
      )
    }
  }

  private fun afterEventManagerSaved(eventManagerSaveResult: SaveResult<EventManagerEntity>) {
    if (eventManagerSaveResult.hasChanged) {
      telemetryService.trackEventManagerAllocated(eventManagerSaveResult.entity)
      sqsSuccessPublisher.updateEvent(eventManagerSaveResult.entity.crn, eventManagerSaveResult.entity.uuid, eventManagerSaveResult.entity.createdDate!!)
    }
  }

  private fun afterRequirementManagersSaved(requirementManagerSaveResults: List<SaveResult<RequirementManagerEntity>>) {
    requirementManagerSaveResults.filter { it.hasChanged }.forEach { saveResult ->
      telemetryService.trackRequirementManagerAllocated(saveResult.entity)
      sqsSuccessPublisher.updateRequirement(saveResult.entity.crn, saveResult.entity.uuid, saveResult.entity.createdDate!!)
    }
  }
}
