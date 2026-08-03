package uk.gov.justice.digital.hmpps.hmppsworkload.service.staff

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.HmppsTierApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
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

  suspend fun getTotalsByTier(staffCode: String, teamCode: String): TierCaseTotals {
    val crns = personManagerRepository.findByStaffCodeAndTeamCodeAndIsActiveIsTrue(staffCode, teamCode)
      .map { it.crn }

    var a = BigDecimal.ZERO
    var b = BigDecimal.ZERO
    var c = BigDecimal.ZERO
    var d = BigDecimal.ZERO
    var e = BigDecimal.ZERO
    var f = BigDecimal.ZERO
    var g = BigDecimal.ZERO
    var missing = BigDecimal.ZERO
    var notSupervised = BigDecimal.ZERO

    for (tier in getTiers(crns)) {
      when (tier) {
        "A" -> a = a.inc()
        "B" -> b = b.inc()
        "C" -> c = c.inc()
        "D" -> d = d.inc()
        "E" -> e = e.inc()
        "F" -> f = f.inc()
        "G" -> g = g.inc()
        "MISSING" -> missing = missing.inc()
        "NOT_SUPERVISED" -> notSupervised = notSupervised.inc()
      }
    }

    return TierCaseTotals(a, b, c, d, e, f, g, missing, notSupervised)
  }

  private suspend fun getTiers(crns: List<String>): List<String> {
    // Tiering API only accepts 20 CRNs at a time, so we have to batch the calls
    return crns.chunked(20).flatMap { tierApiClient.getTierByCrns(it).values }
  }

  private fun teamStaffId(teamCode: String, staffCode: String) = "$teamCode-$staffCode"
}
