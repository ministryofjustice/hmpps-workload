package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping

import java.math.BigInteger

data class TeamOverview(
  var totalCommunityCases: Int,
  var totalLicenseCases: Int,
  var totalCustodyCases: Int,
  val availablePoints: BigInteger,
  val totalPoints: BigInteger,
  val staffCode: String,
  val teamCode: String,
)
