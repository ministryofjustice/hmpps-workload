package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.ReallocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.SaveResult
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity

@Suppress("LongParameterList")
interface SaveEventManagerService {
  fun saveEventManager(teamCode: String, deliusStaff: StaffMember, allocateCase: AllocateCase, loggedInUser: String, spoStaffCode: String, spoName: String): SaveResult<EventManagerEntity>
  fun saveEventManager(teamCode: String, deliusStaff: StaffMember, reallocateCase: ReallocateCase, loggedInUser: String, spoStaffCode: String, spoName: String, eventNumber: Int): SaveResult<EventManagerEntity>
}
