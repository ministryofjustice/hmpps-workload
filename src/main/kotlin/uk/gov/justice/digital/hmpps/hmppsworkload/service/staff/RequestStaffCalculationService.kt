package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WMTWorkloadOwnerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.service.SuccessUpdater
import uk.gov.justice.digital.hmpps.hmppsworkload.service.reduction.GetReductionService

@Service
class RequestStaffCalculationService(
  private val getReductionService: GetReductionService,
  private val successUpdater: SuccessUpdater
) {

  fun requestStaffCalculation(workloadOwner: WMTWorkloadOwnerEntity) {
    val weeklyHours = workloadOwner.contractedHours
    val reductions = getReductionService.findReductionHours(workloadOwner.offenderManager.code, workloadOwner.team.code)
    val availableHours = weeklyHours - reductions
    successUpdater.staffAvailableHoursChange(workloadOwner.offenderManager.code, workloadOwner.team.code, availableHours)
  }
}