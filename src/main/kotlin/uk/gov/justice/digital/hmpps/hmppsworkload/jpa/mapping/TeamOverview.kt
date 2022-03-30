package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping

import java.math.BigDecimal
import java.math.BigInteger

data class TeamOverview(
  val forename: String,
  val surname: String,
  var grade: String,
  val totalCommunityCases: BigDecimal,
  val totalCustodyCases: BigDecimal,
  val availablePoints: BigInteger,
  val totalPoints: BigInteger,
  val code: String,
) {
  var capacity: BigDecimal = BigDecimal.ZERO
  var staffId: Long = -1
}
