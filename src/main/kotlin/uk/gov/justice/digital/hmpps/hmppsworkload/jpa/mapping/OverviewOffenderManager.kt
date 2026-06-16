package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping

import uk.gov.justice.digital.hmpps.hmppsworkload.domain.EventDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.TierCaseTotals
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDateTime
import java.time.ZonedDateTime

data class OverviewOffenderManager(
  val totalCommunityCases: Long,
  val totalLicenseCases: Long,
  val totalCustodyCases: Long,
  val availablePoints: BigInteger,
  val totalPoints: BigInteger,
  val code: String,
  val lastUpdatedOn: LocalDateTime?,
  val workloadOwnerId: Long,
  val paroleReportsDue: BigInteger,
) {
  var capacity: BigDecimal = BigDecimal.ZERO
  var potentialCapacity: BigDecimal? = null
  var nextReductionChange: ZonedDateTime? = null
  var reductionHours: BigDecimal = BigDecimal.ZERO
  var contractedHours: BigDecimal = BigDecimal.ZERO
  var tierCaseTotals: TierCaseTotals = TierCaseTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
  var lastAllocatedEvent: EventDetails? = null
  var hasWorkload = false
}
