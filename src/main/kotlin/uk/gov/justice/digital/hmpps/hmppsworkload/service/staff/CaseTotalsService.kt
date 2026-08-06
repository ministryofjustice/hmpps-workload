package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.TierCaseTotals
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
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

  suspend fun getTeamTotalsByTier(teamCodes: List<String>): Map<String, TierCaseTotals> {
    val cases = personManagerRepository.findByTeamCodeInAndIsActiveIsTrue(teamCodes)
    val tiers = getTiers(cases.map { it.crn })
    val totals = cases
      .groupBy { teamStaffId(it.teamCode, it.staffCode) }
      .mapValues { teamStaffEntry -> calculateTotals(teamStaffEntry.value, tiers) }

    return totals
  }

  suspend fun getPractitionerTotalsByTier(staffCode: String, teamCode: String): TierCaseTotals {
    val cases = personManagerRepository.findByStaffCodeAndTeamCodeAndIsActiveIsTrue(staffCode, teamCode)
    val tiers = getTiers(cases.map { it.crn })
    val totals = calculateTotals(cases, tiers)

    return totals
  }

  private suspend fun getTiers(crns: List<String>): Map<String, String?> {
    val tiers = mutableMapOf<String, String?>()

    // Tiering API only accepts 20 CRNs at a time, so we have to batch the calls
    for (chunk in crns.chunked(20)) {
      tiers += tierApiClient.getTierByCrns(chunk)
    }

    return tiers
  }

  private fun calculateTotals(cases: List<PersonManagerEntity>, tiers: Map<String, String?>): TierCaseTotals {
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
      val tier = tiers.getOrDefault(case.crn, "MISSING")

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
