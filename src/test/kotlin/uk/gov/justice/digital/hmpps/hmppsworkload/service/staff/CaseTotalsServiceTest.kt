package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.TierWithStatus
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Name
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerActiveCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerCases
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.TierCaseTotals
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.PersonManagerRepository
import java.math.BigDecimal
import java.time.LocalDate

private const val TEAM_CODE_1 = "T1"
private const val TEAM_CODE_2 = "T2"
private const val STAFF_CODE_1 = "OM1"
private const val STAFF_CODE_2 = "OM2"

class CaseTotalsServiceTest {
  private val personManagerRepository = mockk<PersonManagerRepository>()
  private val hmppsTierApiClient = mockk<HmppsTierApiClient>()

  private val caseTotalsService = CaseTotalsService(personManagerRepository, hmppsTierApiClient)

  @Test
  fun `getTeamPractitionerTotalsByTier should correctly calculate totals`() {
    runBlocking {
      coEvery { hmppsTierApiClient.getTierByCrns(any()) } returns mapOf(
        "X111111" to TierWithStatus("A", false),
        "X111112" to TierWithStatus("A", false),
        "X111113" to TierWithStatus("B", false),
        "X111114" to TierWithStatus("C", false),
        "X111115" to TierWithStatus("D", false),
        "X111116" to TierWithStatus("E", false),
        "X111117" to TierWithStatus("F", false),
        "X111118" to TierWithStatus("G", false),
        "X111119" to TierWithStatus("MISSING", false),
        "X111120" to TierWithStatus("NOT_SUPERVISED", false),
        "X111121" to TierWithStatus("INVALID_VALUE", false), // Treated as 'MISSING'
        "X111122" to null, // Treated as 'MISSING'
      )

      val totals = caseTotalsService.getTeamPractitionerTotalsByTier(mapOf(StaffIdentifier(STAFF_CODE_1,TEAM_CODE_1) to OffenderManagerCases(
        Name("test", "", "tester"), "PO", STAFF_CODE_1,
        listOf(
          buildActiveCase("X111111"),
          buildActiveCase("X111112"),
          buildActiveCase("X111113"),
          buildActiveCase("X111114"),
          buildActiveCase("X111115"),
          buildActiveCase("X111116"),
        ),
        "test@justice.gov.uk"
      ), StaffIdentifier(STAFF_CODE_2,TEAM_CODE_2) to OffenderManagerCases(
        Name("test2", "", "tester2"), "PO", STAFF_CODE_2,
        listOf(
          buildActiveCase("X111117"),
          buildActiveCase("X111118"),
          buildActiveCase("X111119"),
          buildActiveCase("X111120"),
          buildActiveCase("X111121"),
          buildActiveCase("X111122"),
        ),
        "test@justice.gov.uk"
      )))

      assertEquals(2, totals.size)
      println(totals.toString())
      assertEquals(TierCaseTotals(BigDecimal.valueOf(2), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(0)), totals[STAFF_CODE_1])
      assertEquals(TierCaseTotals(BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(0), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(3), BigDecimal.valueOf(1)), totals[STAFF_CODE_2])
    }
  }

  @Test
  fun `getPractitionerTotalsByTier should correctly calculate totals`() {
    runBlocking {
      coEvery { hmppsTierApiClient.getTierByCrns(any()) } returns mapOf(
        "X111111" to TierWithStatus("A", false),
        "X111112" to TierWithStatus("A", false),
        "X111113" to TierWithStatus("B", false),
        "X111114" to TierWithStatus("C", false),
        "X111115" to TierWithStatus("D", false),
        "X111116" to TierWithStatus("E", false),
        "X111117" to TierWithStatus("F", false),
        "X111118" to TierWithStatus("G", false),
        "X111119" to TierWithStatus("MISSING", false),
        "X111120" to TierWithStatus("NOT_SUPERVISED", false),
        "X111121" to TierWithStatus("INVALID_VALUE", false), // Treated as 'MISSING'
        "X111122" to null, // Treated as 'MISSING'
      )

      val totals = caseTotalsService.getTeamPractitionerTotalsByTier(mapOf(StaffIdentifier(STAFF_CODE_1,TEAM_CODE_1) to OffenderManagerCases(
        Name("test", "", "tester"), "PO", STAFF_CODE_1,
        listOf(
          buildActiveCase("X111111"),
          buildActiveCase("X111112"),
          buildActiveCase("X111113"),
          buildActiveCase("X111114"),
          buildActiveCase("X111115"),
          buildActiveCase("X111116"),
          buildActiveCase("X111117"),
          buildActiveCase("X111118"),
          buildActiveCase("X111119"),
          buildActiveCase("X111120"),
          buildActiveCase("X111121"),
          buildActiveCase("X111122"),
        ),
        "test@justice.gov.uk"
      )))

      assertEquals(TierCaseTotals(BigDecimal.valueOf(2), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(1), BigDecimal.valueOf(3), BigDecimal.valueOf(1)), totals[STAFF_CODE_1])
    }
  }

  private fun buildCase(crn: String, staffCode: String, teamCode: String): PersonManagerEntity = PersonManagerEntity(crn = crn, staffCode = staffCode, teamCode = teamCode, createdBy = teamCode, isActive = true, allocationReason = AllocationReason.INITIAL_ALLOCATION)

  private fun buildActiveCase(crn: String): OffenderManagerActiveCase = OffenderManagerActiveCase(crn, "", false, Name("test", "", "tester"), "", LocalDate.now())
}
