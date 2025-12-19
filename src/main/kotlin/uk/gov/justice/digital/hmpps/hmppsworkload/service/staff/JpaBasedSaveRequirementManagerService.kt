package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Requirement
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.SaveResult
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.RequirementManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.RequirementManagerRepository

@Service
class JpaBasedSaveRequirementManagerService(
  private val requirementManagerRepository: RequirementManagerRepository,
) : SaveRequirementManagerService {

  @Transactional
  override fun saveRequirementManagers(
    teamCode: String,
    deliusStaff: StaffMember,
    allocateCase: AllocateCase,
    loggedInUser: String,
    requirements: List<Requirement>,
  ): List<SaveResult<RequirementManagerEntity>> = requirements
    .map { requirement ->
      requirementManagerRepository.findFirstByCrnAndEventNumberAndRequirementIdOrderByCreatedDateDesc(allocateCase.crn, allocateCase.eventNumber, requirement.id)?.let { requirementManagerEntity ->
        if (requirementManagerEntity.staffCode == deliusStaff.code && requirementManagerEntity.teamCode == teamCode) {
          SaveResult(requirementManagerEntity, false)
        } else {
          requirementManagerEntity.isActive = false
          requirementManagerRepository.save(requirementManagerEntity)
          saveRequirementManagerEntity(allocateCase, deliusStaff, teamCode, loggedInUser, requirement)
        }
      } ?: saveRequirementManagerEntity(allocateCase, deliusStaff, teamCode, loggedInUser, requirement)
    }

  private fun saveRequirementManagerEntity(
    allocateCase: AllocateCase,
    deliusStaff: StaffMember,
    teamCode: String,
    loggedInUser: String,
    requirement: Requirement,
  ): SaveResult<RequirementManagerEntity> {
    val requirementManagerEntity = RequirementManagerEntity(
      crn = allocateCase.crn,
      requirementId = requirement.id,
      staffCode = deliusStaff.code,
      teamCode = teamCode,
      createdBy = loggedInUser,
      isActive = true,
      eventNumber = allocateCase.eventNumber,
      allocationReason = allocateCase.allocationReason,
    )
    requirementManagerRepository.save(requirementManagerEntity)
    return SaveResult(requirementManagerEntity, true)
  }
}
