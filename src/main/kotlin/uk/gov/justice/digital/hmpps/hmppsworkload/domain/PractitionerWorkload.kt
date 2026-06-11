package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ChoosePractitionerResponse
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.CommunityPersonManager
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Name
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ProbationStatus
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.TeamOverview
import uk.gov.justice.digital.hmpps.hmppsworkload.service.calculateCapacity
import java.math.BigDecimal
import java.math.BigInteger

/***
 * Person on probation and practitioner workload
 */
data class PractitionerWorkload @JsonCreator constructor(
  val crn: String,
  val name: Name,
  val tier: Tier,
  val probationStatus: ProbationStatus,
  val communityPersonManager: CommunityPersonManager?,
  val teams: Map<String, List<Practitioner>>,
) {
  companion object {
    fun from(
      choosePractitionerResponse: ChoosePractitionerResponse,
      tier: Tier,
      teams: Map<String, List<Practitioner>>,
    ): PractitionerWorkload = PractitionerWorkload(
      choosePractitionerResponse.crn,
      choosePractitionerResponse.name,
      tier,
      choosePractitionerResponse.probationStatus,
      choosePractitionerResponse.communityPersonManager?.takeUnless { it.isUnallocated },
      teams,
    )
  }
}

data class PractitionerWithRawWorkloadPoints(
  val code: String,
  val name: Name,
  val email: String?,
  val grade: String,
  val workload: BigDecimal,
  val allocatedCasesPastWeek: Int,
  val reallocatedCasesPastWeek: Int,
  val communityCases: Int,
  val custodyCases: Int,
  val availablePoints: BigInteger,
  val totalPoints: BigInteger,
) {
  companion object {
    fun from(staffMember: StaffMember, practitionerWorkload: TeamOverview, allocatedCaseCount: Int, reallocatedCaseCount: Int): PractitionerWithRawWorkloadPoints = PractitionerWithRawWorkloadPoints(
      staffMember.code,
      staffMember.name,
      staffMember.email.takeUnless { email -> email.isNullOrBlank() },
      staffMember.getGrade(),
      calculateCapacity(practitionerWorkload.totalPoints, practitionerWorkload.availablePoints),
      allocatedCaseCount,
      reallocatedCaseCount,
      practitionerWorkload.totalCommunityCases,
      practitionerWorkload.totalCustodyCases,
      practitionerWorkload.availablePoints,
      practitionerWorkload.totalPoints,
    )
  }
}

data class Practitioner constructor(
  val code: String,
  val name: Name,
  val email: String?,
  val grade: String,
  val workload: BigDecimal,
  val allocatedCasesPastWeek: Int,
  val reallocatedCasesPastWeek: Int,
  val communityCases: Int,
  val custodyCases: Int,
) {
  companion object {
    fun from(staffMember: StaffMember, practitionerWorkload: TeamOverview, allocatedCaseCount: Int, reallocatedCaseCount: Int): Practitioner = Practitioner(
      staffMember.code,
      staffMember.name,
      staffMember.email.takeUnless { email -> email.isNullOrBlank() },
      staffMember.getGrade(),
      calculateCapacity(practitionerWorkload.totalPoints, practitionerWorkload.availablePoints),
      allocatedCaseCount,
      reallocatedCaseCount,
      practitionerWorkload.totalCommunityCases,
      practitionerWorkload.totalCustodyCases,
    )
  }
}
