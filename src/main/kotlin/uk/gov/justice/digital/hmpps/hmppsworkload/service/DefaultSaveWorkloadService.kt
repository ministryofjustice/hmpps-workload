package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocationDemandDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.OffenceDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ReallocationDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Requirement
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseAllocated
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseReallocated
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.ReallocateCase
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

@Suppress("LongParameterList")
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
  @Qualifier("hmppsTierApiClient") private val hmppsTierApiClient: HmppsTierApiClient,
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
    val requirementManagerSaveResults = saveRequirementManagerService.saveRequirementManagers(allocatedStaffId.teamCode, allocationData.staff, allocateCase.crn, allocateCase.eventNumber, AllocationReason.INITIAL_ALLOCATION, loggedInUser, unallocatedRequirements)
      .also { afterRequirementManagersSaved(it, caseDetails?.type?.name) }

    try {
      notificationService.notifyAllocation(allocationData, allocateCase, caseDetails)
      log.info("Allocation notified for case: ${allocateCase.crn}, conviction number: ${allocateCase.eventNumber}, to: ${allocationData.staff.code}, from: ${allocationData.allocatingStaff.code}")
      sqsSuccessPublisher.auditAllocation(allocateCase.crn, allocateCase.eventNumber, loggedInUser, unallocatedRequirements.map { it.id })
      log.info("Case allocated: ${allocateCase.crn}, by ${allocationData.allocatingStaff.code}")
      return CaseAllocated(personManagerSaveResult.entity.uuid, eventManagerSaveResult.entity.uuid, requirementManagerSaveResults.map { it.entity.uuid })
    } catch (e: Exception) {
      log.error("Failed to send notification and allocate", e)
    }
    return null
  }

  private fun saveEventManager(allocatedStaffId: StaffIdentifier, allocationData: AllocationDemandDetails, allocateCase: AllocateCase, loggedInUser: String, caseDetails: CaseDetailsEntity): SaveResult<EventManagerEntity> {
    val eventManagerSaveResult = saveEventManagerService.saveEventManager(allocatedStaffId.teamCode, allocationData.staff, allocateCase, loggedInUser, allocationData.allocatingStaff.code, allocationData.allocatingStaff.name.getCombinedName())
      .also { afterEventManagerSaved(it, caseDetails?.type?.name) }
    return eventManagerSaveResult
  }

  @Suppress("LongParameterList")
  private fun saveEventManager(allocatedStaffId: StaffIdentifier, allocationData: AllocationDemandDetails, allocateCase: ReallocateCase, loggedInUser: String, caseType: String?, eventNumber: Int): SaveResult<EventManagerEntity> {
    val eventManagerSaveResult = saveEventManagerService.saveEventManager(allocatedStaffId.teamCode, allocationData.staff, allocateCase, loggedInUser, allocationData.allocatingStaff.code, allocationData.allocatingStaff.name.getCombinedName(), eventNumber)
      .also { afterEventManagerSaved(it, caseType) }
    return eventManagerSaveResult
  }

  private suspend fun savePersonManager(allocatedStaffId: StaffIdentifier, staff: StaffMember, loggedInUser: String, allocateCase: AllocateCase, caseDetails: CaseDetailsEntity): SaveResult<PersonManagerEntity> {
    val personManagerSaveResult = savePersonManagerService.savePersonManager(
      allocatedStaffId.teamCode,
      staff,
      loggedInUser,
      allocateCase.crn,
      AllocationReason.INITIAL_ALLOCATION,
    ).also { afterPersonManagerSaved(it, staff, caseDetails.type?.name, caseDetails.tier?.name) }
    return personManagerSaveResult
  }

  private suspend fun savePersonManager(allocatedStaffId: StaffIdentifier, staff: StaffMember, loggedInUser: String, reason: AllocationReason?, crn: String, type: String?, tier: String?): SaveResult<PersonManagerEntity> {
    val personManagerSaveResult = savePersonManagerService.savePersonManager(
      allocatedStaffId.teamCode,
      staff,
      loggedInUser,
      crn,
      reason,
    ).also { afterPersonManagerSaved(it, staff, type, tier) }
    return personManagerSaveResult
  }

  private fun afterPersonManagerSaved(personManagerSaveResult: SaveResult<PersonManagerEntity>, deliusStaff: StaffMember, type: String?, tier: String?) {
    telemetryService.trackPersonManagerAllocated(personManagerSaveResult.entity, type)
    telemetryService.trackStaffGradeToTierAllocated(tier, deliusStaff, personManagerSaveResult.entity.teamCode)
    sqsSuccessPublisher.updatePerson(
      personManagerSaveResult.entity.crn,
      personManagerSaveResult.entity.uuid,
      personManagerSaveResult.entity.createdDate!!,
    )
  }

  private fun afterEventManagerSaved(eventManagerSaveResult: SaveResult<EventManagerEntity>, caseType: String?) {
    telemetryService.trackEventManagerAllocated(eventManagerSaveResult.entity, caseType)
    sqsSuccessPublisher.updateEvent(eventManagerSaveResult.entity.crn, eventManagerSaveResult.entity.uuid, eventManagerSaveResult.entity.createdDate!!)
  }

  private fun afterRequirementManagersSaved(requirementManagerSaveResults: List<SaveResult<RequirementManagerEntity>>, caseType: String?) {
    requirementManagerSaveResults.filter { it.hasChanged }.forEach { saveResult ->
      telemetryService.trackRequirementManagerAllocated(saveResult.entity, caseType)
      sqsSuccessPublisher.updateRequirement(saveResult.entity.crn, saveResult.entity.uuid, saveResult.entity.createdDate!!)
    }
  }

  @Suppress("LongMethod")
  suspend fun saveReallocatedWorkLoad(
    allocatedStaffId: StaffIdentifier,
    previousStaffCode: String,
    allocateCase: ReallocateCase,
    loggedInUser: String,
  ): CaseReallocated? {
    val caseView = workforceAllocationsToDeliusApiClient.getAllocatedCaseView(allocateCase.crn)
    val tier = hmppsTierApiClient.getTierByCrn(allocateCase.crn)

    // Ensure Previous practitoner has not changed
    val checkPractitioner = workforceAllocationsToDeliusApiClient.getCrnDetails(allocateCase.crn).manager.code

    if (checkPractitioner != previousStaffCode) {
      return null
    }

    val events = caseView.activeEvents.stream().filter { event -> event.sentence != null }.map { event -> event.number }.toList()
    val firstEvent = events[0]

    val eventManagerSaveResults: ArrayList<SaveResult<EventManagerEntity>> = arrayListOf<SaveResult<EventManagerEntity>>()
    val allRequirementManagerSaveResults: ArrayList<SaveResult<RequirementManagerEntity>> = arrayListOf<SaveResult<RequirementManagerEntity>>()

    var allocationData = workforceAllocationsToDeliusApiClient.allocationDetails(allocateCase.crn, firstEvent, allocatedStaffId.staffCode, loggedInUser)

    val personManagerSaveResult = savePersonManager(allocatedStaffId, allocationData.staff, loggedInUser, allocateCase.allocationReason, allocateCase.crn, "", tier)
    val allUnallocatedRequirements = arrayListOf<Requirement>()
    val allOffences = arrayListOf<OffenceDetails>()

    for (event in events) {
      allocationData = workforceAllocationsToDeliusApiClient.allocationDetails(allocateCase.crn, event, allocatedStaffId.staffCode, loggedInUser)

      val eventManagerSaveResult = saveEventManager(allocatedStaffId, allocationData, allocateCase, loggedInUser, "", event)
      val unallocatedRequirements = allocationData.activeRequirements.filter { !it.manager.allocated || it.manager.code == previousStaffCode }
      val offences = allocationData.offences
      val requirementManagerSaveResults = saveRequirementManagerService.saveRequirementManagers(allocatedStaffId.teamCode, allocationData.staff, allocateCase.crn, event, allocateCase.allocationReason!!, loggedInUser, unallocatedRequirements)
        .also { afterRequirementManagersSaved(it, "") }
      eventManagerSaveResults.addLast(eventManagerSaveResult)
      allRequirementManagerSaveResults.addAll(requirementManagerSaveResults)
      allUnallocatedRequirements.addAll(allocationData.activeRequirements)
      allOffences.addAll(offences)
    }

    val reallocationNotificationDetails = getAdditionalNotificationDetails(previousStaffCode, allocateCase, allUnallocatedRequirements, allOffences)

    try {
      notificationService.notifyReallocation(allocationData, allocateCase, tier, reallocationNotificationDetails)
      log.info("Reallocation notified for case: ${allocateCase.crn}, to: ${allocationData.staff.code}, from: ${allocationData.allocatingStaff.code}")
      sqsSuccessPublisher.auditAllocation(allocateCase.crn, null, loggedInUser, allUnallocatedRequirements.map { it.id })
      log.info("Case reallocated: ${allocateCase.crn}, by ${allocationData.allocatingStaff.code}")
      return CaseReallocated(personManagerSaveResult.entity.uuid, eventManagerSaveResults.map { it.entity.uuid }, allRequirementManagerSaveResults.map { it.entity.uuid })
    } catch (e: Exception) {
      log.error("Failed to send notification and allocate", e)
    }
    return null
  }

  private suspend fun getAdditionalNotificationDetails(previousStaffCode: String, allocateCase: ReallocateCase, requirements: List<Requirement>, offences: List<OffenceDetails>): ReallocationDetails {
    val previousStaff = workforceAllocationsToDeliusApiClient.getOfficerView(previousStaffCode)
    val reallocationDetails = ReallocationDetails(
      toText(allocateCase.allocationReason!!),
      allocateCase.lastOasysAssessmentDate,
      allocateCase.nextAppointmentDate,
      allocateCase.failureToComply,
      StaffMember(previousStaff.code, previousStaff.name, previousStaff.email, previousStaff.getGrade()),
      requirements,
      offences,
    )
    return reallocationDetails
  }

  private fun toText(reason: AllocationReason) = reason.toString().capitalize().replace("_", " ")

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
