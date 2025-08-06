package uk.gov.justice.digital.hmpps.hmppsworkload.integration.team

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.WorkforceAllocationsToDeliusExtension.Companion.workforceAllocationsToDelius
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import java.time.ZonedDateTime

class ChoosePractitionersByTeamCodes : IntegrationTestBase() {

  @Test
  fun `can get choose practitioner response`() {
    val teamCode = "T1"
    val teamCode2 = "T2"
    val crn = "CRN1"
    val caseDetails = caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.B3, CaseType.CUSTODY, "Don", "Cole"))

    workforceAllocationsToDelius.choosePractitionerByTeamCodesResponse(listOf(teamCode, teamCode2), crn)
    val firstWmtStaff = setupCurrentWmtStaff("OM1", teamCode)
    val secondWmtStaff = setupCurrentWmtStaff("OM2", teamCode2)

    val firstOm = firstWmtStaff.offenderManager.code
    val secondOm = secondWmtStaff.offenderManager.code
    val noWorkloadStaffCode = "NOWORKLOAD1"

    val storedPersonManager = PersonManagerEntity(crn = "CRN5", staffCode = firstOm, teamCode = teamCode, createdBy = "USER1", isActive = true)
    personManagerRepository.save(storedPersonManager)

    val movedPersonManager = PersonManagerEntity(crn = "CRN3", staffCode = firstOm, teamCode = teamCode, createdBy = "USER1", createdDate = ZonedDateTime.now().minusDays(5L), isActive = false)
    personManagerRepository.save(movedPersonManager)

    val newPersonManager = PersonManagerEntity(crn = "CRN3", staffCode = secondOm, teamCode = teamCode2, createdBy = "USER2", createdDate = ZonedDateTime.now().minusDays(2L), isActive = true)
    personManagerRepository.save(newPersonManager)

    val personManagerWithNoWorkload = PersonManagerEntity(crn = "CRN4", staffCode = noWorkloadStaffCode, teamCode = "T1", createdBy = "USER2", createdDate = ZonedDateTime.now().minusDays(2L), isActive = true)
    personManagerRepository.save(personManagerWithNoWorkload)

    webTestClient.get()
      .uri("/team/choose-practitioner?crn=$crn&teamCodes=$teamCode,$teamCode2")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.crn")
      .isEqualTo(crn)
      .jsonPath("$.name.forename")
      .isEqualTo("Don")
      .jsonPath("$.name.middleName")
      .isEqualTo("")
      .jsonPath("$.name.surname")
      .isEqualTo("Cole")
      .jsonPath("$.tier")
      .isEqualTo(caseDetails.tier.name)
      .jsonPath("$.probationStatus.status")
      .isEqualTo("PREVIOUSLY_MANAGED")
      .jsonPath("$.probationStatus.description")
      .isEqualTo("Previously managed")
      .jsonPath("$.communityPersonManager.code")
      .isEqualTo("N03A019")
      .jsonPath("$.communityPersonManager.name.forename")
      .isEqualTo("Derek")
      .jsonPath("$.communityPersonManager.name.surname")
      .isEqualTo("Pint")
      .jsonPath("$.communityPersonManager.teamCode")
      .isEqualTo("N03F01")
      .jsonPath("$.communityPersonManager.grade")
      .isEqualTo("PO")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].name.forename")
      .isEqualTo("Jane")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].name.surname")
      .isEqualTo("Doe")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].email")
      .isEqualTo("j.doe@email.co.uk")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].grade")
      .isEqualTo("PO")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].workload")
      .isEqualTo(50.toDouble())
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].casesPastWeek")
      .isEqualTo(1)
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].communityCases")
      .isEqualTo(15)
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].custodyCases")
      .isEqualTo(20)
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].name.forename")
      .isEqualTo("No")
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].name.surname")
      .isEqualTo("Workload")
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].email")
      .isEqualTo("no.workload.email@co.uk")
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].workload")
      .isEqualTo(0)
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].casesPastWeek")
      .isEqualTo(1)
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].communityCases")
      .isEqualTo(0)
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].custodyCases")
      .isEqualTo(0)
      .jsonPath("$.teams.$teamCode2[?(@.code == '$secondOm')].name.forename")
      .isEqualTo("Mark")
      .jsonPath("$.teams.$teamCode2[?(@.code == '$secondOm')].name.surname")
      .isEqualTo("Reese")
      .jsonPath("$.teams.$teamCode2[?(@.code == '$secondOm')].email")
      .doesNotExist()
      .jsonPath("$.teams.$teamCode2[?(@.code == '$secondOm')].grade")
      .isEqualTo("PQiP")
  }

  @Test
  fun `can get choose practitioner response team only`() {
    val teamCode = "T1"
    val teamCode2 = "T2"
    val crn = "CRN1"
    val caseDetails = caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.B3, CaseType.CUSTODY, "Don", "Cole"))

    workforceAllocationsToDelius.choosePractitionerByTeamCodesResponse(listOf(teamCode, teamCode2), crn)
    val firstWmtStaff = setupCurrentWmtStaff("OM1", teamCode)
    val secondWmtStaff = setupCurrentWmtStaff("OM2", teamCode2)

    val firstOm = firstWmtStaff.offenderManager.code
    val secondOm = secondWmtStaff.offenderManager.code
    val noWorkloadStaffCode = "NOWORKLOAD1"

    val storedPersonManager = PersonManagerEntity(crn = "CRN5", staffCode = firstOm, teamCode = teamCode, createdBy = "USER1", isActive = true)
    personManagerRepository.save(storedPersonManager)

    val movedPersonManager = PersonManagerEntity(crn = "CRN3", staffCode = firstOm, teamCode = teamCode, createdBy = "USER1", createdDate = ZonedDateTime.now().minusDays(5L), isActive = false)
    personManagerRepository.save(movedPersonManager)

    val newPersonManager = PersonManagerEntity(crn = "CRN3", staffCode = secondOm, teamCode = teamCode2, createdBy = "USER2", createdDate = ZonedDateTime.now().minusDays(2L), isActive = true)
    personManagerRepository.save(newPersonManager)

    val personManagerWithNoWorkload = PersonManagerEntity(crn = "CRN4", staffCode = noWorkloadStaffCode, teamCode = "T1", createdBy = "USER2", createdDate = ZonedDateTime.now().minusDays(2L), isActive = true)
    personManagerRepository.save(personManagerWithNoWorkload)

    webTestClient.get()
      .uri("/team/choose-practitioner?&teamCodes=$teamCode,$teamCode2&crn=$crn")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.crn")
      .isEqualTo(crn)
      .jsonPath("$.name.forename")
      .isEqualTo("Don")
      .jsonPath("$.name.middleName")
      .isEqualTo("")
      .jsonPath("$.name.surname")
      .isEqualTo("Cole")
      .jsonPath("$.tier")
      .isEqualTo(caseDetails.tier.name)
      .jsonPath("$.probationStatus.status")
      .isEqualTo("PREVIOUSLY_MANAGED")
      .jsonPath("$.probationStatus.description")
      .isEqualTo("Previously managed")
      .jsonPath("$.communityPersonManager.code")
      .isEqualTo("N03A019")
      .jsonPath("$.communityPersonManager.name.forename")
      .isEqualTo("Derek")
      .jsonPath("$.communityPersonManager.name.surname")
      .isEqualTo("Pint")
      .jsonPath("$.communityPersonManager.teamCode")
      .isEqualTo("N03F01")
      .jsonPath("$.communityPersonManager.grade")
      .isEqualTo("PO")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].name.forename")
      .isEqualTo("Jane")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].name.surname")
      .isEqualTo("Doe")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].email")
      .isEqualTo("j.doe@email.co.uk")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].grade")
      .isEqualTo("PO")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].workload")
      .isEqualTo(50.toDouble())
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].casesPastWeek")
      .isEqualTo(1)
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].communityCases")
      .isEqualTo(15)
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].custodyCases")
      .isEqualTo(20)
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].name.forename")
      .isEqualTo("No")
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].name.surname")
      .isEqualTo("Workload")
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].email")
      .isEqualTo("no.workload.email@co.uk")
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].workload")
      .isEqualTo(0)
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].casesPastWeek")
      .isEqualTo(1)
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].communityCases")
      .isEqualTo(0)
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')].custodyCases")
      .isEqualTo(0)
      .jsonPath("$.teams.$teamCode2[?(@.code == '$secondOm')].name.forename")
      .isEqualTo("Mark")
      .jsonPath("$.teams.$teamCode2[?(@.code == '$secondOm')].name.surname")
      .isEqualTo("Reese")
      .jsonPath("$.teams.$teamCode2[?(@.code == '$secondOm')].email")
      .doesNotExist()
      .jsonPath("$.teams.$teamCode2[?(@.code == '$secondOm')].grade")
      .isEqualTo("PQiP")
  }

  @Test
  fun `can get choose practitioner response team code only`() {
    val teamCode = "T1"

    workforceAllocationsToDelius.choosePractitionerByTeamCodesResponseNoPoP(listOf(teamCode))
    val firstWmtStaff = setupCurrentWmtStaff("OM1", teamCode)

    val firstOm = firstWmtStaff.offenderManager.code

    val storedPersonManager = PersonManagerEntity(crn = "CRN5", staffCode = firstOm, teamCode = teamCode, createdBy = "USER1", isActive = true)
    personManagerRepository.save(storedPersonManager)

    val movedPersonManager = PersonManagerEntity(crn = "CRN3", staffCode = firstOm, teamCode = teamCode, createdBy = "USER1", createdDate = ZonedDateTime.now().minusDays(5L), isActive = false)
    personManagerRepository.save(movedPersonManager)

    webTestClient.get()
      .uri("/team/practitioner-workloadcases?teamCode=$teamCode")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.$teamCode.teams[?(@.code == '$firstOm')].name.forename")
      .isEqualTo("Jane")
      .jsonPath("$.$teamCode.teams[?(@.code == '$firstOm')].name.surname")
      .isEqualTo("Doe")
      .jsonPath("$.$teamCode.teams[?(@.code == '$firstOm')].email")
      .isEqualTo("j.doe@email.co.uk")
      .jsonPath("$.$teamCode.teams[?(@.code == '$firstOm')].grade")
      .isEqualTo("PO")
      .jsonPath("$.$teamCode.teams[?(@.code == '$firstOm')].workload")
      .isEqualTo(50.toDouble())
      .jsonPath("$.$teamCode.teams[?(@.code == '$firstOm')].casesPastWeek")
      .isEqualTo(1)
      .jsonPath("$.$teamCode.teams[?(@.code == '$firstOm')].communityCases")
      .isEqualTo(15)
      .jsonPath("$.$teamCode.teams[?(@.code == '$firstOm')].custodyCases")
      .isEqualTo(20)
  }

  @Test
  fun `throws 404 when crn not in repository`() {
    val teamCode = "T1"
    val teamCode2 = "T2"
    val crn = "NOTFOUND"

    workforceAllocationsToDelius.choosePractitionerByTeamCodesResponse(listOf(teamCode, teamCode2), crn)
    val firstWmtStaff = setupCurrentWmtStaff("OM1", teamCode)
    val secondWmtStaff = setupCurrentWmtStaff("OM2", teamCode2)

    val firstOm = firstWmtStaff.offenderManager.code
    val secondOm = secondWmtStaff.offenderManager.code
    val noWorkloadStaffCode = "NOWORKLOAD1"

    val storedPersonManager = PersonManagerEntity(crn = "CRN5", staffCode = firstOm, teamCode = teamCode, createdBy = "USER1", isActive = true)
    personManagerRepository.save(storedPersonManager)

    val movedPersonManager = PersonManagerEntity(crn = "CRN3", staffCode = firstOm, teamCode = teamCode, createdBy = "USER1", createdDate = ZonedDateTime.now().minusDays(5L), isActive = false)
    personManagerRepository.save(movedPersonManager)

    val newPersonManager = PersonManagerEntity(crn = "CRN3", staffCode = secondOm, teamCode = teamCode2, createdBy = "USER2", createdDate = ZonedDateTime.now().minusDays(2L), isActive = true)
    personManagerRepository.save(newPersonManager)

    val personManagerWithNoWorkload = PersonManagerEntity(crn = "CRN4", staffCode = noWorkloadStaffCode, teamCode = "T1", createdBy = "USER2", createdDate = ZonedDateTime.now().minusDays(2L), isActive = true)
    personManagerRepository.save(personManagerWithNoWorkload)

    webTestClient.get()
      .uri("/team/choose-practitioner?crn=$crn&teamCodes=$teamCode,$teamCode2")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `can filter officers by grade`() {
    val teamCode = "T1"
    val teamCode2 = "T2"
    val crn = "CRN1"
    caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.B3, CaseType.CUSTODY, "Don", "Cole"))
    workforceAllocationsToDelius.choosePractitionerByTeamCodesResponse(listOf(teamCode, teamCode2), crn)
    val firstWmtStaff = setupCurrentWmtStaff("OM1", teamCode)
    setupCurrentWmtStaff("OM2", teamCode2)

    val firstOm = firstWmtStaff.offenderManager.code
    val noWorkloadStaffCode = "NOWORKLOAD1"

    webTestClient.get()
      .uri("/team/choose-practitioner?crn=$crn&teamCodes=$teamCode,$teamCode2&grades=PO,PQiP")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].name.forename")
      .isEqualTo("Jane")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].name.surname")
      .isEqualTo("Doe")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].email")
      .isEqualTo("j.doe@email.co.uk")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].grade")
      .isEqualTo("PO")
      .jsonPath("$.teams.$teamCode[?(@.code == '$noWorkloadStaffCode')]")
      .doesNotExist()
  }

  @Test
  fun `must return not found when team code is not matched`() {
    webTestClient.get()
      .uri("/team/choose-practitioner?crn=CRNRANDOM123456&teamCodes=T1")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `must return forbidden when auth token does not contain correct role`() {
    val teamCode = "T1"
    val teamCode2 = "T2"
    val crn = "CRN1"
    webTestClient.get()
      .uri("/team/choose-practitioner?crn=$crn&teamCodes=$teamCode,$teamCode2&grades=PO,PQiP")
      .headers { it.authToken(roles = listOf("ROLE_RANDOM_ROLE")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `can get team overview of offender manager by team code when assigned to another staff member`() {
    val teamCode = "T1"
    val staffCode = "OM1"
    val crn = "CRN1"
    caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.B3, CaseType.CUSTODY, "Don", "Cole"))
    workforceAllocationsToDelius.choosePractitionerByTeamCodesResponse(listOf(teamCode), crn)

    val movedPersonManager = PersonManagerEntity(crn = "CRN3", staffCode = staffCode, teamCode = teamCode, createdBy = "USER1", isActive = false)
    personManagerRepository.save(movedPersonManager)

    val newPersonManager = PersonManagerEntity(crn = "CRN3", staffCode = "STAFF2", teamCode = "TEAM2", createdBy = "USER2", isActive = true)
    personManagerRepository.save(newPersonManager)

    webTestClient.get()
      .uri("/team/choose-practitioner?crn=$crn&teamCodes=$teamCode")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.teams.$teamCode[?(@.code == '$staffCode')].casesPastWeek")
      .isEqualTo(0)
  }

  @Test
  fun `must get correct workloads when the same offender manager is working in multiple teams`() {
    val teamCode = "T1"
    val teamCode2 = "T2"
    val crn = "CRN1"
    caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.B3, CaseType.CUSTODY, "Don", "Cole"))
    val staffCode = "OM1"

    workforceAllocationsToDelius.choosePractitionerStaffInMultipleTeamsResponse(listOf(teamCode, teamCode2), crn)
    val firstTeamWorkload = setupCurrentWmtStaff(staffCode, teamCode, 20)
    val secondTeamWorkload = setupCurrentWmtStaff(staffCode, teamCode2, 50)

    webTestClient.get()
      .uri("/team/choose-practitioner?crn=$crn&teamCodes=$teamCode,$teamCode2")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.teams.$teamCode[?(@.code == '$staffCode')].custodyCases")
      .isEqualTo(firstTeamWorkload.workload.totalFilteredCustodyCases)
      .jsonPath("$.teams.$teamCode2[?(@.code == '$staffCode')].custodyCases")
      .isEqualTo(secondTeamWorkload.workload.totalFilteredCustodyCases)
  }

  @Test
  fun `can get choose practitioner response when practitioner does not have an email field`() {
    val teamCode = "T2"
    val crn = "CRN1"
    caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.B3, CaseType.CUSTODY, "Don", "Cole"))

    workforceAllocationsToDelius.choosePractitionerByTeamCodesResponse(listOf(teamCode), crn)
    val firstWmtStaff = setupCurrentWmtStaff("OM3", teamCode)

    val firstOm = firstWmtStaff.offenderManager.code

    val storedPersonManager = PersonManagerEntity(crn = "CRN1", staffCode = firstOm, teamCode = teamCode, createdBy = "USER1", isActive = true)
    personManagerRepository.save(storedPersonManager)

    webTestClient.get()
      .uri("/team/choose-practitioner?crn=$crn&teamCodes=$teamCode")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].name.forename")
      .isEqualTo("Mark")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].name.surname")
      .isEqualTo("NoEmail")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].email")
      .doesNotExist()
  }

  @Test
  fun `can get choose practitioner response when there is no community person manager`() {
    val teamCode = "T2"
    val crn = "CRN1"
    caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.B3, CaseType.CUSTODY, "Don", "Cole"))

    workforceAllocationsToDelius.choosePractitionerByTeamCodesResponseNoCommunityPersonManager(listOf(teamCode), crn)
    val firstWmtStaff = setupCurrentWmtStaff("OM3", teamCode)

    val firstOm = firstWmtStaff.offenderManager.code

    val storedPersonManager = PersonManagerEntity(crn = "CRN1", staffCode = firstOm, teamCode = teamCode, createdBy = "USER1", isActive = true)
    personManagerRepository.save(storedPersonManager)

    webTestClient.get()
      .uri("/team/choose-practitioner?crn=$crn&teamCodes=$teamCode")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].name.forename")
      .isEqualTo("Mark")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].name.surname")
      .isEqualTo("NoEmail")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].email")
      .doesNotExist()
      .jsonPath("$.communityPersonManager")
      .doesNotExist()
  }

  @Test
  fun `can get choose practitioner response when unallocated case`() {
    val teamCode = "T2"
    val crn = "CRN1"
    caseDetailsRepository.save(CaseDetailsEntity(crn, Tier.B3, CaseType.CUSTODY, "Don", "Cole"))

    workforceAllocationsToDelius.choosePractitionerByTeamCodesResponseUnallocated(listOf(teamCode), crn)
    val firstWmtStaff = setupCurrentWmtStaff("OM3", teamCode)

    val firstOm = firstWmtStaff.offenderManager.code

    val storedPersonManager = PersonManagerEntity(crn = "CRN1", staffCode = firstOm, teamCode = teamCode, createdBy = "USER1", isActive = true)
    personManagerRepository.save(storedPersonManager)

    webTestClient.get()
      .uri("/team/choose-practitioner?crn=$crn&teamCodes=$teamCode")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].name.forename")
      .isEqualTo("Mark")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].name.surname")
      .isEqualTo("NoEmail")
      .jsonPath("$.teams.$teamCode[?(@.code == '$firstOm')].email")
      .doesNotExist()
      .jsonPath("$.communityPersonManager")
      .doesNotExist()
  }
}
