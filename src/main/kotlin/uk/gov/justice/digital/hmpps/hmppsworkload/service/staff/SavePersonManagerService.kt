package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.SaveResult
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity

interface SavePersonManagerService {

  fun savePersonManager(teamCode: String, deliusStaff: StaffMember, loggedInUser: String, crn: String): SaveResult<PersonManagerEntity>
}
