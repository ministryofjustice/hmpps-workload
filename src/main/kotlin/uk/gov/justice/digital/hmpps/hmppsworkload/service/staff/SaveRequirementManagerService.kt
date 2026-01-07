package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Requirement
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.SaveResult
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.RequirementManagerEntity

interface SaveRequirementManagerService {
  fun saveRequirementManagers(teamCode: String, deliusStaff: StaffMember, crn: String, eventNumber: Int, reason: AllocationReason, loggedInUser: String, requirements: List<Requirement>): List<SaveResult<RequirementManagerEntity>>
}
