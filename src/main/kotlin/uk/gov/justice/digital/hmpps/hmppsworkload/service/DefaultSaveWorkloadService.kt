package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseAllocated
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.SaveEventManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.SavePersonManagerService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.SaveRequirementManagerService

@Service
class DefaultSaveWorkloadService(
  private val savePersonManagerService: SavePersonManagerService,
  private val communityApiClient: CommunityApiClient,
  private val saveEventManagerService: SaveEventManagerService,
  private val saveRequirementManagerService: SaveRequirementManagerService,
  private val notificationService: NotificationService
) : SaveWorkloadService {

  override fun saveWorkload(
    teamCode: String,
    staffCode: String,
    allocateCase: AllocateCase,
    loggedInUser: String,
    authToken: String
  ): CaseAllocated {
    val staff = communityApiClient.getStaffByCode(staffCode).block()!!
    val summary = communityApiClient.getSummaryByCrn(allocateCase.crn).block()!!
    val activeRequirements = communityApiClient.getActiveRequirements(allocateCase.crn, allocateCase.eventId).block()!!.requirements
    val personManagerSaveResult = savePersonManagerService.savePersonManager(teamCode, staff, allocateCase, loggedInUser, summary)
    val eventManagerSaveResult = saveEventManagerService.saveEventManager(teamCode, staff, allocateCase, loggedInUser)
    val requirementManagerSaveResults = saveRequirementManagerService.saveRequirementManagers(teamCode, staff, allocateCase, loggedInUser, activeRequirements)
    if (personManagerSaveResult.hasChanged || eventManagerSaveResult.hasChanged || requirementManagerSaveResults.any { it.hasChanged }) {
      notificationService.notifyAllocation(staff, summary, activeRequirements, allocateCase, loggedInUser, authToken)
        .block()
    }

    return CaseAllocated(personManagerSaveResult.entity.uuid, eventManagerSaveResult.entity.uuid, requirementManagerSaveResults.map { it.entity.uuid })
  }
}
