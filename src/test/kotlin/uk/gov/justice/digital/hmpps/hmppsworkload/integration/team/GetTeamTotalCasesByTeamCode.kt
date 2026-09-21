package uk.gov.justice.digital.hmpps.hmppsworkload.integration.team

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.TeamOverview
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.HmppsProbationEstateApiExtension.Companion.hmppsProbationEstate

class GetTeamTotalCasesByTeamCode : IntegrationTestBase() {
  val teamCode = "T1"

  @Test
  fun `can get cases by team code`() {
    setupCasesForTeamMember("OM1", teamCode)
    setupCasesForTeamMember("OM2", teamCode)

    setupReportData()

    hmppsProbationEstate.getTeamsResponse(listOf(TeamOverview(teamCode, "Team 1")))

    webTestClient.get()
      .uri("/team/workloadcases?teams=$teamCode")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT")) }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.[0].teamCode")
      .isEqualTo("T1")
      .jsonPath("$.[0].totalCases")
      .isEqualTo(7)
  }

  @Test
  fun `must return forbidden when auth token does not contain correct role`() {
    webTestClient.get()
      .uri("/team/workloadcases?teams=$teamCode")
      .headers { it.authToken(roles = listOf("ROLE_RANDOM_ROLE")) }
      .exchange()
      .expectStatus()
      .isForbidden
  }
}
