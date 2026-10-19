package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ActiveCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerCases
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.TierCaseTotals
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.PersonManagerRepository
import java.math.BigDecimal
import java.time.ZonedDateTime

@Service
class CaseTotalsService(
  private val personManagerRepository: PersonManagerRepository,
  private val tierApiClient: HmppsTierApiClient,
) {
  fun getPractitionerAllocationCaseCounts(teamCodes: List<String>, caseCountAfter: ZonedDateTime): Map<String, Int> = personManagerRepository.findByTeamCodeInAndCreatedDateGreaterThanEqualAndIsActiveIsTrue(teamCodes, caseCountAfter)
    .filter { it.allocationReason == AllocationReason.INITIAL_ALLOCATION }
    .groupBy { teamStaffId(it.teamCode, it.staffCode) }
    .mapValues { countEntry -> countEntry.value.size }

  fun getPractitionerReallocationCaseCounts(teamCodes: List<String>, caseCountAfter: ZonedDateTime): Map<String, Int> = personManagerRepository.findByTeamCodeInAndCreatedDateGreaterThanEqualAndIsActiveIsTrue(teamCodes, caseCountAfter)
    .filter { it.allocationReason != AllocationReason.INITIAL_ALLOCATION }
    .groupBy { teamStaffId(it.teamCode, it.staffCode) }
    .mapValues { countEntry -> countEntry.value.size }

  fun getPractitionerAllocationCaseCountsTeamCodeOnly(teamCodes: List<String>, caseCountAfter: ZonedDateTime): Map<String, Int> = personManagerRepository.findByTeamCodeInAndCreatedDateGreaterThanEqualAndIsActiveIsTrue(teamCodes, caseCountAfter)
    .filter { it.allocationReason == AllocationReason.INITIAL_ALLOCATION }
    .groupBy { it.staffCode }
    .mapValues { countEntry -> countEntry.value.size }

  fun getPractitionerReallocationCaseCountsTeamCodeOnly(teamCodes: List<String>, caseCountAfter: ZonedDateTime): Map<String, Int> = personManagerRepository.findByTeamCodeInAndCreatedDateGreaterThanEqualAndIsActiveIsTrue(teamCodes, caseCountAfter)
    .filter { it.allocationReason != AllocationReason.INITIAL_ALLOCATION }
    .groupBy { it.staffCode }
    .mapValues { countEntry -> countEntry.value.size }

  suspend fun getTeamPractitionerTotals(teamCodes: List<String>): Map<String, Int> {
    val cases = personManagerRepository.findByTeamCodeInAndIsActiveIsTrue(teamCodes)
    val totals = cases
      .groupBy { teamStaffId(it.teamCode, it.staffCode) }
      .mapValues { entry -> entry.value.size }

    return totals
  }

  suspend fun getTeamPractitionerTotalsByTier(cases: Map<StaffIdentifier, OffenderManagerCases?>): Map<String, TierCaseTotals> {
    val teamTierCaseTotals = mutableMapOf<String, TierCaseTotals>()
    cases.forEach { (staffIdentifier, cases) ->
      if (cases != null) {
        val tiers = getTiers(cases.activeCases.map { it.crn })
        teamTierCaseTotals[cases.code] = calculateTotals(
          cases.activeCases.map { it.crn },
          tiers,
        )
      }
    }
    return teamTierCaseTotals
  }

  suspend fun getPractitionerTotalsByTier(activeCases: List<ActiveCase>): TierCaseTotals {
    val tiers = getTiers(activeCases.map { it.crn })
    val totals = calculateTotals(activeCases.map { it.crn }, tiers)

    return totals
  }

  private suspend fun getTiers(crns: List<String>): Map<String, String?> {
    val tiers = mutableMapOf<String, String?>()

    // Tiering API only accepts 20 CRNs at a time, so we have to batch the calls
    for (chunk in crns.chunked(20)) {
      tiers += tierApiClient.getTierByCrns(chunk).mapValues { it.value?.tierScore }
    }

    return tiers
  }

  private fun calculateTotals(cases: List<String>, tiers: Map<String, String?>): TierCaseTotals {
    var a = BigDecimal.ZERO
    var b = BigDecimal.ZERO
    var c = BigDecimal.ZERO
    var d = BigDecimal.ZERO
    var e = BigDecimal.ZERO
    var f = BigDecimal.ZERO
    var g = BigDecimal.ZERO
    var missing = BigDecimal.ZERO
    var notSupervised = BigDecimal.ZERO

    for (case in cases) {
      val tier = tiers.getOrDefault(case, "MISSING")

      when (tier) {
        "A" -> a = a.inc()
        "B" -> b = b.inc()
        "C" -> c = c.inc()
        "D" -> d = d.inc()
        "E" -> e = e.inc()
        "F" -> f = f.inc()
        "G" -> g = g.inc()
        "NOT_SUPERVISED" -> notSupervised = notSupervised.inc()
        else -> missing = missing.inc()
      }
    }

    return TierCaseTotals(a, b, c, d, e, f, g, missing, notSupervised)
  }

  private fun teamStaffId(teamCode: String, staffCode: String) = "$teamCode-$staffCode"
}
