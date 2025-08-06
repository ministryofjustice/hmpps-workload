package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.PersonSummary
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.PersonManager
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.GetPersonManager

@Service
class SaveCaseDetailsService(
  @Qualifier("hmppsTierApiClient") private val hmppsTierApiClient: HmppsTierApiClient,
  private val caseDetailsRepository: CaseDetailsRepository,
  private val workloadCalculationService: WorkloadCalculationService,
  private val getPersonManager: GetPersonManager,
  private val updateWorkloadService: UpdateWorkloadService,
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {

  suspend fun saveByCrn(crn: String) {
    val personSummary = workforceAllocationsToDeliusApiClient.getPersonByCrn(crn)
    savePerson(personSummary)
  }

  suspend fun saveByNoms(noms: String) {
    val personSummary = workforceAllocationsToDeliusApiClient.getPersonByNoms(noms)
    savePerson(personSummary)
  }

  private suspend fun savePerson(
    personSummary: PersonSummary?,
  ) {
    log.info("Entered savePerson() for crn: ${personSummary?.crn} with case type: ${personSummary?.type}")
    personSummary?.takeUnless { it.type == CaseType.UNKNOWN }?.type?.let { caseType ->
      hmppsTierApiClient.getTierByCrn(personSummary.crn)?.let {
        val tier = Tier.valueOf(it)
        val caseDetails = CaseDetailsEntity(personSummary.crn, tier, caseType, personSummary.name.forename, personSummary.name.surname)
        caseDetailsRepository.save(caseDetails)
        val staff: PersonManager? = getPersonManager.findLatestByCrn(personSummary.crn)
        log.info("PersonManager in savePerson() = $staff for crn ${personSummary.crn}")
        if (staff != null) {
          workloadCalculationService.saveWorkloadCalculation(
            StaffIdentifier(staff.staffCode, staff.teamCode),
            staff.staffGrade,
          )
        }
      }
    } ?: personSummary?.crn?.let { crn ->
      log.info("CaseType UNKNOWN for crn: $crn")
      caseDetailsRepository.findByIdOrNull(crn)?.let {
        log.info("Deleting caseDetails for crn $crn")
        caseDetailsRepository.delete(it)
        updateWorkloadService.setWorkloadInactive(crn)
      }
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
