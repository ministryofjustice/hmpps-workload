package uk.gov.justice.digital.hmpps.hmppsworkload.integration.reduction

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.entity.ReductionCategoryEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.entity.ReductionReasonEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.ReductionEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.ReductionStatus
import java.math.BigDecimal
import java.time.ZonedDateTime

class ReductionPerformanceIssue : IntegrationTestBase() {

  @Test
  fun `query must only return reductions associated with the workload owner`() {
    val wmtStaff = setupCurrentWmtStaff("STAFF2", "TEAM2")

    val otherWmtStaff = setupCurrentWmtStaff("OTHERSTAFF", "OTHERTEAM")

    val reductionCategory = reductionCategoryRepository.save(ReductionCategoryEntity())
    val reductionReason = reductionReasonRepository.save(ReductionReasonEntity(reductionCategoryEntity = reductionCategory))
    val expectedReduction = reductionsRepository.save(ReductionEntity(workloadOwner = wmtStaff.wmtWorkloadOwnerEntity, hours = BigDecimal.valueOf(5), effectiveFrom = ZonedDateTime.now().plusDays(3), effectiveTo = ZonedDateTime.now().plusDays(7), status = ReductionStatus.SCHEDULED, reductionReasonId = reductionReason.id!!))

    reductionsRepository.save(ReductionEntity(workloadOwner = otherWmtStaff.wmtWorkloadOwnerEntity, hours = BigDecimal.valueOf(5), effectiveFrom = ZonedDateTime.now().plusDays(5), effectiveTo = ZonedDateTime.now().plusDays(7), status = ReductionStatus.SCHEDULED, reductionReasonId = reductionReason.id!!))

    val result = reductionsRepository.findUpcomingReductions(
      wmtStaff.wmtWorkloadOwnerEntity,
      listOf(ReductionStatus.DELETED, ReductionStatus.ARCHIVED),
      ZonedDateTime.now(),
      ZonedDateTime.now(),
    )

    Assertions.assertThat(result).extracting("id").containsOnly(expectedReduction.id!!)
  }
}
