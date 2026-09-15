package uk.gov.justice.digital.hmpps.hmppsworkload.domain

import com.fasterxml.jackson.annotation.JsonCreator
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ChoosePractitionerResponse
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.CommunityPersonManager
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Name
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ProbationStatus
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember

/***
 * Person on probation and practitioner workload
 */
data class PractitionerWorkload @JsonCreator constructor(
  val crn: String,
  val name: Name,
  val tier: Tier,
  val provisionalTier: Boolean,
  val probationStatus: ProbationStatus,
  val communityPersonManager: CommunityPersonManager?,
  val teams: Map<String, List<Practitioner>>,
) {
  companion object {
    fun from(
      choosePractitionerResponse: ChoosePractitionerResponse,
      tier: Tier,
      provisionalTier: Boolean,
      teams: Map<String, List<Practitioner>>,
    ): PractitionerWorkload = PractitionerWorkload(
      choosePractitionerResponse.crn,
      choosePractitionerResponse.name,
      tier,
      provisionalTier,
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
  val casesPastWeek: Int,
  val allocatedCasesPastWeek: Int,
  val reallocatedCasesPastWeek: Int,
  val totalCases: Int,
  val communityCases: Int,
  val licenseCases: Int,
  val custodyCases: Int,
  val ispsDueInNext14Days: Int,
  val activeCases: Int,
  val contactSuspendedCases: Int,
  val custodyReleasesInNext7Days: Int,
  val paroleReportsInNext28Days: Int,
  val otherReportsInNext14Days: Int,
  val tierCaseTotals: TierCaseTotals?,
) {
  companion object {
    fun from(staffMember: StaffMember, practitionerCases: OffenderManagerCases, practitionerStats: PractitionerStats): PractitionerWithRawWorkloadPoints = PractitionerWithRawWorkloadPoints(
      staffMember.code,
      staffMember.name,
      staffMember.email.takeUnless { email -> email.isNullOrBlank() },
      staffMember.getGrade(),
      practitionerStats.allocatedCaseCount + practitionerStats.reallocatedCaseCount,
      practitionerStats.allocatedCaseCount,
      practitionerStats.reallocatedCaseCount,
      practitionerCases.activeCases.size + practitionerStats.contactSuspendedCases,
      practitionerCases.activeCases.filter { it.type == "LICENSE" }.size,
      practitionerCases.activeCases.filter { it.type == "COMMUNITY" }.size,
      practitionerCases.activeCases.filter { it.type == "CUSTODY" }.size,
      practitionerStats.ispsDueInNext14Days,
      practitionerCases.activeCases.size,
      practitionerStats.contactSuspendedCases,
      practitionerStats.custodyReleasesInNext7Days,
      practitionerStats.paroleReportsInNext28Days,
      practitionerStats.hdcrotlReportsInNext14Days + practitionerStats.partBReportsInNext14Days + practitionerStats.partCReportsInNext14Days,
      practitionerStats.tierCaseTotals,
    )
  }
}

data class Practitioner constructor(
  val code: String,
  val name: Name,
  val email: String?,
  val grade: String,
  val casesPastWeek: Int,
  val allocatedCasesPastWeek: Int,
  val reallocatedCasesPastWeek: Int,
  val totalCases: Int,
  val communityCases: Int,
  val licenseCases: Int,
  val custodyCases: Int,
  val ispsDueInNext14Days: Int,
  val activeCases: Int,
  val contactSuspendedCases: Int,
  val custodyReleasesInNext7Days: Int,
  val paroleReportsInNext28Days: Int,
  val otherReportsInNext14Days: Int,
  val tierCaseTotals: TierCaseTotals?,
) {
  companion object {
    fun from(staffMember: StaffMember, practitionerCases: OffenderManagerCases, practitionerStats: PractitionerStats): Practitioner = Practitioner(
      staffMember.code,
      staffMember.name,
      staffMember.email.takeUnless { email -> email.isNullOrBlank() },
      staffMember.getGrade(),
      practitionerStats.allocatedCaseCount + practitionerStats.reallocatedCaseCount,
      practitionerStats.allocatedCaseCount,
      practitionerStats.reallocatedCaseCount,
      practitionerCases.activeCases.size + practitionerStats.contactSuspendedCases,
      practitionerCases.activeCases.filter { it.type == "LICENSE" }.size,
      practitionerCases.activeCases.filter { it.type == "COMMUNITY" }.size,
      practitionerCases.activeCases.filter { it.type == "CUSTODY" }.size,
      practitionerStats.ispsDueInNext14Days,
      practitionerCases.activeCases.size,
      practitionerStats.contactSuspendedCases,
      practitionerStats.custodyReleasesInNext7Days,
      practitionerStats.paroleReportsInNext28Days,
      practitionerStats.hdcrotlReportsInNext14Days + practitionerStats.partBReportsInNext14Days + practitionerStats.partCReportsInNext14Days,
      practitionerStats.tierCaseTotals,
    )
  }
}

private fun getActiveCases(communityCases: Int, licenseCases: Int, custodyCases: Int, contactSuspendedCases: Int): Int {
  val activeCases = communityCases + licenseCases + custodyCases - contactSuspendedCases
  return if (activeCases < 0) {
    0
  } else {
    activeCases
  }
}
