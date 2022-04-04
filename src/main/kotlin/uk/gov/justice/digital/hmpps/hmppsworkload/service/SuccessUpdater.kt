package uk.gov.justice.digital.hmpps.hmppsworkload.service

import java.time.ZonedDateTime
import java.util.UUID

interface SuccessUpdater {

  fun updatePerson(crn: String, allocationId: UUID, timeUpdated: ZonedDateTime)
}
