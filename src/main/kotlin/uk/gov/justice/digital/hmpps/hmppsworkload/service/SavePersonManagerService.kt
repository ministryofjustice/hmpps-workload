package uk.gov.justice.digital.hmpps.hmppsworkload.service

import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.PersonSummary
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Staff
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity

interface SavePersonManagerService {

  fun savePersonManager(teamCode: String, staff: Staff, allocateCase: AllocateCase, loggedInUser: String, personSummary: PersonSummary): PersonManagerEntity
}
