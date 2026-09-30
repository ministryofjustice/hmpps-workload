package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Case
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.OffenderManagerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.PersonManagerRepository

@Service
class GetCombinedCaseload(
  private val offenderManagerRepository: OffenderManagerRepository,
  private val personManagerRepository: PersonManagerRepository,
  private val caseDetailsRepository: CaseDetailsRepository,
) {
  /***
   * Get combined Cases from WMT (offenderManagerRepository) and realtime cases (personManagerRepository).
   */
  fun getCases(staffIdentifier: StaffIdentifier): List<Case> = caseDetailsRepository.findAllById(
    offenderManagerRepository.findCasesByTeamCodeAndStaffCode(staffIdentifier.staffCode, staffIdentifier.teamCode)
      .union(
        personManagerRepository.findByStaffCodeAndTeamCodeAndIsActiveIsTrue(
          staffIdentifier.staffCode,
          staffIdentifier.teamCode,
        ).map { c -> c.crn },
      ),
  )
    .map { cde -> Case(cde.tier, cde.provisionalTier, cde.type, false, cde.crn) }
}
