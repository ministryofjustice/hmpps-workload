package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocationDemandDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ReallocationDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Requirement
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseAllocated
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseReallocated
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.SaveResult
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.RequirementManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.SaveEventManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.SavePersonManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.SaveRequirementManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.utils.capitalize

@Service
class DefaultSaveWorkloadService(
  private val savePersonManagerService: SavePersonManagerService,
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  private val saveEventManagerService: SaveEventManagerService,
  private val saveRequirementManagerService: SaveRequirementManagerService,
  private val notificationService: NotificationService,
  private val telemetryService: TelemetryService,
  private val sqsSuccessPublisher: SqsSuccessPublisher,
  private val caseDetailsRepository: CaseDetailsRepository,
) {

  @Suppress("TooGenericExceptionCaught")
  suspend fun saveWorkload(
    allocatedStaffId: StaffIdentifier,
    allocateCase: AllocateCase,
    loggedInUser: String,
  ): CaseAllocated? {
    val caseDetails: CaseDetailsEntity = caseDetailsRepository.findByIdOrNull(allocateCase.crn)!!
    val allocationData = workforceAllocationsToDeliusApiClient.allocationDetails(allocateCase.crn, allocateCase.eventNumber, allocatedStaffId.staffCode, loggedInUser)
    val personManagerSaveResult = savePersonManager(allocatedStaffId, allocationData.staff, loggedInUser, allocateCase, caseDetails)
    val eventManagerSaveResult = saveEventManager(allocatedStaffId, allocationData, allocateCase, loggedInUser, caseDetails)

    val unallocatedRequirements = allocationData.activeRequirements.filter { !it.manager.allocated }
    val requirementManagerSaveResults = saveRequirementManagerService.saveRequirementManagers(allocatedStaffId.teamCode, allocationData.staff, allocateCase, loggedInUser, unallocatedRequirements)
      .also { afterRequirementManagersSaved(it, caseDetails) }

    try {
      notificationService.notifyAllocation(allocationData, allocateCase, caseDetails)
      log.info("Allocation notified for case: ${caseDetails.crn}, conviction number: ${allocateCase.eventNumber}, to: ${allocationData.staff.code}, from: ${allocationData.allocatingStaff.code}")
      sqsSuccessPublisher.auditAllocation(allocateCase.crn, allocateCase.eventNumber, loggedInUser, unallocatedRequirements.map { it.id })
      log.info("Case allocated: ${caseDetails.crn}, by ${allocationData.allocatingStaff.code}")
      return CaseAllocated(personManagerSaveResult.entity.uuid, eventManagerSaveResult.entity.uuid, requirementManagerSaveResults.map { it.entity.uuid })
    } catch (e: Exception) {
      log.error("Failed to send notification and allocate", e)
    }
    return null
  }

  private fun saveEventManager(allocatedStaffId: StaffIdentifier, allocationData: AllocationDemandDetails, allocateCase: AllocateCase, loggedInUser: String, caseDetails: CaseDetailsEntity): SaveResult<EventManagerEntity> {
    val eventManagerSaveResult = saveEventManagerService.saveEventManager(allocatedStaffId.teamCode, allocationData.staff, allocateCase, loggedInUser, allocationData.allocatingStaff.code, allocationData.allocatingStaff.name.getCombinedName())
      .also { afterEventManagerSaved(it, caseDetails) }
    return eventManagerSaveResult
  }

  private suspend fun savePersonManager(allocatedStaffId: StaffIdentifier, staff: StaffMember, loggedInUser: String, allocateCase: AllocateCase, caseDetails: CaseDetailsEntity): SaveResult<PersonManagerEntity> {
    val personManagerSaveResult = savePersonManagerService.savePersonManager(
      allocatedStaffId.teamCode,
      staff,
      loggedInUser,
      allocateCase.crn,
      allocateCase.allocationReason,
    ).also { afterPersonManagerSaved(it, staff, caseDetails) }
    return personManagerSaveResult
  }

  private fun afterPersonManagerSaved(personManagerSaveResult: SaveResult<PersonManagerEntity>, deliusStaff: StaffMember, caseDetails: CaseDetailsEntity) {
    telemetryService.trackPersonManagerAllocated(personManagerSaveResult.entity, caseDetails)
    telemetryService.trackStaffGradeToTierAllocated(caseDetails, deliusStaff, personManagerSaveResult.entity.teamCode)
    sqsSuccessPublisher.updatePerson(
      personManagerSaveResult.entity.crn,
      personManagerSaveResult.entity.uuid,
      personManagerSaveResult.entity.createdDate!!,
    )
  }

  private fun afterEventManagerSaved(eventManagerSaveResult: SaveResult<EventManagerEntity>, caseDetails: CaseDetailsEntity) {
    telemetryService.trackEventManagerAllocated(eventManagerSaveResult.entity, caseDetails)
    sqsSuccessPublisher.updateEvent(eventManagerSaveResult.entity.crn, eventManagerSaveResult.entity.uuid, eventManagerSaveResult.entity.createdDate!!)
  }

  private fun afterRequirementManagersSaved(requirementManagerSaveResults: List<SaveResult<RequirementManagerEntity>>, caseDetails: CaseDetailsEntity) {
    requirementManagerSaveResults.filter { it.hasChanged }.forEach { saveResult ->
      telemetryService.trackRequirementManagerAllocated(saveResult.entity, caseDetails)
      sqsSuccessPublisher.updateRequirement(saveResult.entity.crn, saveResult.entity.uuid, saveResult.entity.createdDate!!)
    }
  }

  suspend fun saveReallocatedWorkLoad(
    allocatedStaffId: StaffIdentifier,
    previousStaffCode: String,
    allocateCase: AllocateCase,
    loggedInUser: String,
  ): CaseReallocated? {
    val caseDetails: CaseDetailsEntity = caseDetailsRepository.findByIdOrNull(allocateCase.crn)!!

    val caseView = workforceAllocationsToDeliusApiClient.getAllocatedCaseView(allocateCase.crn)

    // Ensure Previous practitoner has not changed
    val checkPractitioner = workforceAllocationsToDeliusApiClient.getCrnDetails(allocateCase.crn).manager.code

    if (checkPractitioner != previousStaffCode) {
      return null
    }

    val firstEvent = caseView.activeEvents.stream().map { event -> event.number }.findFirst().get()
    val events = caseView.activeEvents.stream().map { event -> event.number }

    val eventManagerSaveResults: ArrayList<SaveResult<EventManagerEntity>> = arrayListOf<SaveResult<EventManagerEntity>>()
    val allRequirementManagerSaveResults: ArrayList<SaveResult<RequirementManagerEntity>> = arrayListOf<SaveResult<RequirementManagerEntity>>()

    var allocationData = workforceAllocationsToDeliusApiClient.allocationDetails(allocateCase.crn, firstEvent, allocatedStaffId.staffCode, loggedInUser)

    val personManagerSaveResult = savePersonManager(allocatedStaffId, allocationData.staff, loggedInUser, allocateCase, caseDetails)
    val allUnallocatedRequirements = arrayListOf<Requirement>()

    for (event in events) {
      allocationData = workforceAllocationsToDeliusApiClient.allocationDetails(allocateCase.crn, event, allocatedStaffId.staffCode, loggedInUser)

      val eventManagerSaveResult = saveEventManager(allocatedStaffId, allocationData, allocateCase, loggedInUser, caseDetails)
      val unallocatedRequirements = allocationData.activeRequirements.filter { !it.manager.allocated || it.manager.code == previousStaffCode }
      val requirementManagerSaveResults = saveRequirementManagerService.saveRequirementManagers(allocatedStaffId.teamCode, allocationData.staff, allocateCase, loggedInUser, unallocatedRequirements)
        .also { afterRequirementManagersSaved(it, caseDetails) }
      eventManagerSaveResults.addLast(eventManagerSaveResult)
      allRequirementManagerSaveResults.addAll(requirementManagerSaveResults)
      allUnallocatedRequirements.addAll(unallocatedRequirements)
    }

    val previousStaff = workforceAllocationsToDeliusApiClient.getOfficerView(previousStaffCode)
    val reallocationDetails = ReallocationDetails(
      toText(allocateCase.allocationReason!!),
      allocateCase.lastOasysAssessmentDate,
      allocateCase.nextAppointmentDate,
      allocateCase.failureToComply,
      StaffMember(previousStaff.code, previousStaff.name, previousStaff.email, previousStaff.getGrade()),
    )

    try {
      notificationService.notifyReallocation(allocationData, allocateCase, caseDetails, reallocationDetails)
      log.info("Reallocation notified for case: ${caseDetails.crn}, to: ${allocationData.staff.code}, from: ${allocationData.allocatingStaff.code}")
      sqsSuccessPublisher.auditAllocation(allocateCase.crn, null, loggedInUser, allUnallocatedRequirements.map { it.id })
      log.info("Case reallocated: ${caseDetails.crn}, by ${allocationData.allocatingStaff.code}")
      return CaseReallocated(personManagerSaveResult.entity.uuid, eventManagerSaveResults.map { it.entity.uuid }, allRequirementManagerSaveResults.map { it.entity.uuid })
    } catch (e: Exception) {
      log.error("Failed to send notification and allocate", e)
    }
    return null
  }

  private fun toText(reason: AllocationReason) = reason.toString().capitalize().replace("_", " ")

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
