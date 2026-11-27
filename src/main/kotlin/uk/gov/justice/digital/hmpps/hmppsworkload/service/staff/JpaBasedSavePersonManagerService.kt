package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.SaveResult
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.PersonManagerRepository

@Service
class JpaBasedSavePersonManagerService(
  private val personManagerRepository: PersonManagerRepository,
) : SavePersonManagerService {
  @Transactional
  override suspend fun savePersonManager(
    teamCode: String,
    deliusStaff: StaffMember,
    loggedInUser: String,
    crn: String,
    allocationReason: AllocationReason?,
  ): SaveResult<PersonManagerEntity> = personManagerRepository.findFirstByCrnOrderByCreatedDateDesc(crn)?.let { personManager ->
    if (personManager.staffCode == deliusStaff.code && personManager.teamCode == teamCode) {
      SaveResult(personManager, false)
    } else {
      createPersonManager(deliusStaff, teamCode, loggedInUser, crn, allocationReason).also {
        personManager.isActive = false
        personManagerRepository.save(personManager)
      }
    }
  } ?: createPersonManager(deliusStaff, teamCode, loggedInUser, crn, allocationReason)

  private fun createPersonManager(
    deliusStaff: StaffMember,
    teamCode: String,
    loggedInUser: String,
    crn: String,
    allocationReason: AllocationReason?,
  ): SaveResult<PersonManagerEntity> {
    val personManagerEntity = PersonManagerEntity(
      crn = crn,
      staffCode = deliusStaff.code,
      teamCode = teamCode,
      createdBy = loggedInUser,
      isActive = true,
      allocationReason = allocationReason,
    )
    personManagerRepository.save(personManagerEntity)
    return SaveResult(personManagerEntity, true)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
