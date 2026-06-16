package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping

import java.math.BigInteger

data class TeamOverview(
  val totalCommunityCases: Int,
  val totalLicenseCases: Int,
  val totalCustodyCases: Int,
  val availablePoints: BigInteger,
  val totalPoints: BigInteger,
  val staffCode: String,
  val teamCode: String,
)
