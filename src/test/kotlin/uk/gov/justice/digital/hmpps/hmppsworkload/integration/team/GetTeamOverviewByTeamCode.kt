package uk.gov.justice.digital.hmpps.hmppsworkload.integration.team

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import java.math.BigInteger
import java.time.ZonedDateTime

class GetTeamOverviewByTeamCode : IntegrationTestBase() {

  @Test
  fun `can get team overview of offender managers by team code`() {
    val teamCode = "T1"
    teamStaffResponse(teamCode)
    val firstWmtStaff = setupCurrentWmtStaff("OM1", teamCode)
    val secondWmtStaff = setupCurrentWmtStaff("OM2", teamCode)

    val storedPersonManager = PersonManagerEntity(crn = "CRN1", staffId = BigInteger.valueOf(123456789L), staffCode = firstWmtStaff.offenderManager.code, teamCode = teamCode, offenderName = "John Doe", createdBy = "USER1", providerCode = "R1", isActive = true)
    personManagerRepository.save(storedPersonManager)

    val movedPersonManager = PersonManagerEntity(crn = "CRN3", staffId = BigInteger.valueOf(123456789L), staffCode = firstWmtStaff.offenderManager.code, teamCode = teamCode, offenderName = "John Doe", createdBy = "USER1", providerCode = "R1", createdDate = ZonedDateTime.now().minusDays(5L), isActive = false)
    personManagerRepository.save(movedPersonManager)

    val newPersonManager = PersonManagerEntity(crn = "CRN3", staffId = BigInteger.valueOf(56789321L), staffCode = secondWmtStaff.offenderManager.code, teamCode = teamCode, offenderName = "John Doe", createdBy = "USER2", providerCode = "R1", createdDate = ZonedDateTime.now().minusDays(2L), isActive = true)
    personManagerRepository.save(newPersonManager)

    val personManagerWithNoWorkload = PersonManagerEntity(crn = "CRN4", staffId = BigInteger.valueOf(56789321L), staffCode = "NOWORKLOAD1", teamCode = "T1", offenderName = "John Doe", createdBy = "USER2", providerCode = "R1", createdDate = ZonedDateTime.now().minusDays(2L), isActive = true)
    personManagerRepository.save(personManagerWithNoWorkload)

    webTestClient.get()
      .uri("/team/$teamCode/offenderManagers")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].forename")
      .isEqualTo(firstWmtStaff.offenderManager.forename)
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].surname")
      .isEqualTo(firstWmtStaff.offenderManager.surname)
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].grade")
      .isEqualTo("PO")
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].totalCommunityCases")
      .isEqualTo(15)
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].totalCustodyCases")
      .isEqualTo(20)
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].capacity")
      .isEqualTo(50.toDouble())
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].staffId")
      .isEqualTo(123456789)
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].totalCasesInLastWeek")
      .isEqualTo(1)
      .jsonPath("$.offenderManagers[?(@.code == 'NOWORKLOAD1')].forename")
      .isEqualTo("Jane")
      .jsonPath("$.offenderManagers[?(@.code == 'NOWORKLOAD1')].surname")
      .isEqualTo("Doe")
      .jsonPath("$.offenderManagers[?(@.code == 'NOWORKLOAD1')].grade")
      .isEqualTo("DMY")
      .jsonPath("$.offenderManagers[?(@.code == 'NOWORKLOAD1')].totalCasesInLastWeek")
      .isEqualTo(1)
      .jsonPath("$.offenderManagers[?(@.code == 'NOWORKLOAD1')].totalCommunityCases")
      .isEqualTo(0)
      .jsonPath("$.offenderManagers[?(@.code == 'NOWORKLOAD1')].totalCustodyCases")
      .isEqualTo(0)
      .jsonPath("$.offenderManagers[?(@.code == 'NOWORKLOAD1')].capacity")
      .isEqualTo(0)
      .jsonPath("$.offenderManagers[?(@.code == 'NOWORKLOAD1')].staffId")
      .isEqualTo(987654321)
      .jsonPath("$.offenderManagers[?(@.code == '${secondWmtStaff.offenderManager.code}')].grade")
      .isEqualTo("PQiP")
  }

  @Test
  fun `can filter officers by grade`() {
    val teamCode = "T1"
    teamStaffResponse(teamCode)
    val firstWmtStaff = setupCurrentWmtStaff("OM1", teamCode)
    val secondWmtStaff = setupCurrentWmtStaff("OM2", teamCode)
    webTestClient.get()
      .uri("/team/$teamCode/offenderManagers?grades=PO,PQiP")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].forename")
      .isEqualTo("Jane")
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].surname")
      .isEqualTo("Doe")
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].grade")
      .isEqualTo("PO")
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].totalCommunityCases")
      .isEqualTo(15)
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].totalCustodyCases")
      .isEqualTo(20)
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].capacity")
      .isEqualTo(50.toDouble())
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].code")
      .isEqualTo("OM1")
      .jsonPath("$.offenderManagers[?(@.code == '${firstWmtStaff.offenderManager.code}')].staffId")
      .isEqualTo(123456789)
      .jsonPath("$.offenderManagers[?(@.code == 'NOWORKLOAD1')]")
      .doesNotExist()
  }

  @Test
  fun `must return not found when team code is not matched`() {
    webTestClient.get()
      .uri("/team/RANDOMCODE/offenderManagers")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isNotFound
  }

  @Test
  fun `must return forbidden when auth token does not contain correct role`() {
    webTestClient.get()
      .uri("/team/T1/offenderManagers")
      .headers { it.authToken(roles = listOf("ROLE_RANDOM_ROLE")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `can get team overview of offender manager by team code when assigned to another staff member`() {
    val teamCode = "TEAM1"
    teamStaffResponse(teamCode, "STAFF1")

    val movedPersonManager = PersonManagerEntity(crn = "CRN3", staffId = BigInteger.valueOf(123456789L), staffCode = "STAFF1", teamCode = teamCode, offenderName = "John Doe", createdBy = "USER1", providerCode = "R1", isActive = false)
    personManagerRepository.save(movedPersonManager)

    val newPersonManager = PersonManagerEntity(crn = "CRN3", staffId = BigInteger.valueOf(56789321L), staffCode = "STAFF2", teamCode = "TEAM2", offenderName = "John Doe", createdBy = "USER2", providerCode = "R1", isActive = true)
    personManagerRepository.save(newPersonManager)

    webTestClient.get()
      .uri("/team/$teamCode/offenderManagers")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.offenderManagers[?(@.code == 'STAFF1')].totalCasesInLastWeek")
      .isEqualTo(0)
  }
}
