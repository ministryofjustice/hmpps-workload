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

private const val TEAM_CODE_1 = "T1"
private const val TEAM_CODE_2 = "T2"
private const val STAFF_CODE_1 = "OM1"
private const val STAFF_CODE_2 = "OM2"

class CaseTotalsServiceTest {
  private val personManagerRepository = mockk<PersonManagerRepository>()
  private val hmppsTierApiClient = mockk<HmppsTierApiClient>()

  private val caseTotalsService = CaseTotalsService(personManagerRepository, hmppsTierApiClient)

  @Test
  fun `getTeamTotalsByTier should correctly calculate totals`() {
    runBlocking {
      coEvery { personManagerRepository.findByTeamCodeInAndIsActiveIsTrue(listOf(TEAM_CODE_1, TEAM_CODE_2)) } returns listOf(
        buildCase("X111111", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111112", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111113", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111114", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111115", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111116", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111117", STAFF_CODE_2, TEAM_CODE_2),
        buildCase("X111118", STAFF_CODE_2, TEAM_CODE_2),
        buildCase("X111119", STAFF_CODE_2, TEAM_CODE_2),
        buildCase("X111120", STAFF_CODE_2, TEAM_CODE_2),
        buildCase("X111121", STAFF_CODE_2, TEAM_CODE_2),
        buildCase("X111122", STAFF_CODE_2, TEAM_CODE_2),
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
        "X111121" to "INVALID_VALUE", // Treated as 'MISSING'
        "X111121" to null, // Treated as 'MISSING'
      )

      val totals = caseTotalsService.getTeamTotalsByTier(listOf(TEAM_CODE_1, TEAM_CODE_2))

      assertEquals(2, totals.size)
      assertEquals(TierCaseTotals(BigDecimal.valueOf(2), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(0)), totals["$TEAM_CODE_1-$STAFF_CODE_1"])
      assertEquals(TierCaseTotals(BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(3), BigDecimal.valueOf(1)), totals["$TEAM_CODE_2-$STAFF_CODE_2"])
    }
  }

  @Test
  fun `getPractitionerTotalsByTier should correctly calculate totals`() {
    runBlocking {
      coEvery { personManagerRepository.findByStaffCodeAndTeamCodeAndIsActiveIsTrue(STAFF_CODE_1, TEAM_CODE_1) } returns listOf(
        buildCase("X111111", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111112", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111113", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111114", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111115", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111116", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111117", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111118", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111119", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111120", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111121", STAFF_CODE_1, TEAM_CODE_1),
        buildCase("X111122", STAFF_CODE_1, TEAM_CODE_1),
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
        "X111121" to "INVALID_VALUE", // Treated as 'MISSING'
        "X111121" to null, // Treated as 'MISSING'
      )

      val totals = caseTotalsService.getPractitionerTotalsByTier(STAFF_CODE_1, TEAM_CODE_1)

      assertEquals(TierCaseTotals(BigDecimal.valueOf(2), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(3), BigDecimal.valueOf(1)), totals)
    }
  }

  private fun buildCase(crn: String, staffCode: String, teamCode: String): PersonManagerEntity = PersonManagerEntity(crn = crn, staffCode = staffCode, teamCode = teamCode, createdBy = teamCode, isActive = true, allocationReason = AllocationReason.INITIAL_ALLOCATION)
}
