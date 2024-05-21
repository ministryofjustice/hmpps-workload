package uk.gov.justice.digital.hmpps.hmppsworkload.integration.eventManager

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class GetAllocatedEventsCount : IntegrationTestBase() {

  private val loggedInUser = "SOME_USER"

  @Test
  fun `can get count of all allocated events by logged in user`() {
    val storedEventManager = eventManagerRepository.save(
      EventManagerEntity(
        crn = "CRN1",
        staffCode = "OM1",
        teamCode = "T1",
        createdBy = loggedInUser,
        isActive = true,
        eventNumber = 2,
        spoStaffId = "SP2",
        spoName = "Fred flintstone",
        spoTeamCode = "Earth",
      ),
    )

    caseDetailsRepository.save(CaseDetailsEntity(crn = storedEventManager.crn, tier = Tier.B2, type = CaseType.COMMUNITY, "", ""))

    webTestClient.get()
      .uri(
        "/allocation/events/me/count?since=${thirtyDaysInPast()}",
      )
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_READ", loggedInUser))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.caseCount")
      .isEqualTo(1)
  }

  @Test
  fun `only get count since`() {
    val oldEventManager = eventManagerRepository.save(
      EventManagerEntity(
        crn = "CRN1",
        staffCode = "OM1",
        teamCode = "T1",
        createdBy = loggedInUser,
        isActive = true,
        eventNumber = 2,
        spoStaffId = "SP2",
        spoName = "Fred flintstone",
        spoTeamCode = "Earth",
      ),
    )
    oldEventManager.createdDate = ZonedDateTime.now().minusDays(60)
    eventManagerRepository.save(oldEventManager)

    caseDetailsRepository.save(CaseDetailsEntity(crn = oldEventManager.crn, tier = Tier.B2, type = CaseType.COMMUNITY, "", ""))

    webTestClient.get()
      .uri(
        "/allocation/events/me/count?since=${thirtyDaysInPast()}",
      )
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_READ", loggedInUser))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.caseCount")
      .isEqualTo(0)
  }

  @Test
  fun `only retrieve active event managers`() {
    eventManagerRepository.save(
      EventManagerEntity(
        crn = "CRN1",
        staffCode = "OM1",
        teamCode = "T1",
        createdBy = loggedInUser,
        isActive = false,
        eventNumber = 2,
        spoStaffId = "SP2",
        spoName = "Fred flintstone",
        spoTeamCode = "Earth",
      ),
    )

    webTestClient.get()
      .uri(
        "/allocation/events/me/count?since=${thirtyDaysInPast()}",
      )
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_READ", loggedInUser))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.caseCount")
      .isEqualTo(0)
  }

  private fun thirtyDaysInPast(): String? =
    ZonedDateTime.now().minusDays(30).truncatedTo(ChronoUnit.DAYS).withZoneSameInstant(ZoneOffset.UTC).format(
      DateTimeFormatter.ISO_OFFSET_DATE_TIME,
    )
}
