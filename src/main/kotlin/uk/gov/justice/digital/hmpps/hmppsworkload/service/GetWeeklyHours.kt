package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WMTWorkloadOwnerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WorkloadPointsRepository
import java.math.BigDecimal

@Service
class GetWeeklyHours(private val wmtWorkloadOwnerRepository: WMTWorkloadOwnerRepository, private val workloadPointsRepository: WorkloadPointsRepository) {

  fun findWeeklyHours(staffIdentifier: StaffIdentifier, staffGrade: String): BigDecimal = (
    wmtWorkloadOwnerRepository.findFirstByOffenderManagerCodeAndTeamCodeOrderByIdDesc(staffIdentifier.staffCode, staffIdentifier.teamCode)?.contractedHours
      ?: getDefaultWeeklyHoursForGrade(staffGrade)
    ).stripTrailingZeros()

  private fun getDefaultWeeklyHoursForGrade(staffGrade: String): BigDecimal = workloadPointsRepository.findFirstByIsT2AAndEffectiveToIsNullOrderByEffectiveFromDesc(false)
    .getDefaultContractedHours(staffGrade)
}
