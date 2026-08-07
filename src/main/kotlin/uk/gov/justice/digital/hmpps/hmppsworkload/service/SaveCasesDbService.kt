package uk.gov.justice.digital.hmpps.hmppsworkload.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.UpdatedCaseDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository

@Service
class SaveCasesDbService(
  private val caseDetailsRepository: CaseDetailsRepository,
) {
  @Transactional
  fun insertCaseDetails(details: UpdatedCaseDetails) {
    caseDetailsRepository.insertCaseDetails(details.firstName, details.surname, details.tier.name, details.provisionalTier, details.caseType.name, details.crn)
  }
}
