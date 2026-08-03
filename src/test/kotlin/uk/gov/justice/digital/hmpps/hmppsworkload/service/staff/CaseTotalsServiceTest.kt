package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.TierCaseTotals
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.PersonManagerRepository
import java.math.BigDecimal

private const val STAFF_CODE = "001"
private const val TEAM_CODE = "REDS"

class CaseTotalsServiceTest {
  private val personManagerRepository = mockk<PersonManagerRepository>()
  private val hmppsTierApiClient = mockk<HmppsTierApiClient>()

  private val caseTotalsService = CaseTotalsService(personManagerRepository, hmppsTierApiClient)

  @Test
  fun `getTotalsByTier should correctly calculate totals`() {
    runBlocking {
      coEvery { personManagerRepository.findByStaffCodeAndTeamCodeAndIsActiveIsTrue(STAFF_CODE, TEAM_CODE) } returns listOf(
        buildCase("X111111"),
        buildCase("X111112"),
        buildCase("X111113"),
        buildCase("X111114"),
        buildCase("X111115"),
        buildCase("X111116"),
        buildCase("X111117"),
        buildCase("X111118"),
        buildCase("X111119"),
        buildCase("X111120"),
        buildCase("X111121"),
      )

      coEvery { hmppsTierApiClient.getTierByCrns(any()) } returns mapOf(
        "X111111" to "A",
        "X111112" to "A",
        "X111113" to "B",
        "X111114" to "C",
        "X111115" to "D",
        "X111116" to "E",
        "X111117" to "F",
        "X111118" to "G",
        "X111119" to "MISSING",
        "X111120" to "NOT_SUPERVISED",
        "X111121" to "INVALID_VALUE",
      )

      val totals = caseTotalsService.getTotalsByTier(STAFF_CODE, TEAM_CODE)

      assertEquals(TierCaseTotals(BigDecimal.valueOf(2), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1)), totals)
    }
  }

  private fun buildCase(crn: String): PersonManagerEntity = PersonManagerEntity(crn = crn, staffCode = STAFF_CODE, teamCode = TEAM_CODE, createdBy = TEAM_CODE, isActive = true, allocationReason = AllocationReason.INITIAL_ALLOCATION)
}
