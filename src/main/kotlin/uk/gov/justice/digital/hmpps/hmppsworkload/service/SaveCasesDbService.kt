package uk.gov.justice.digital.hmpps.hmppsworkload.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository

@Service
class SaveCasesDbService(
  private val caseDetailsRepository: CaseDetailsRepository,
) {
  @Transactional
  fun insertCaseDetails(firstName: String, surname: String, tier: Tier, caseType: CaseType, crn: String) {
    caseDetailsRepository.insertCaseDetails(firstName, surname, tier.name, caseType.name, crn)
  }
}
