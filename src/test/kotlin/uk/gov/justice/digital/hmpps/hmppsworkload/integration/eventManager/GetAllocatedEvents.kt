package uk.gov.justice.digital.hmpps.hmppsworkload.integration.eventManager

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.domain.AllocationDetailIntegration
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.WorkforceAllocationsToDeliusExtension.Companion.workforceAllocationsToDelius
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class GetAllocatedEvents : IntegrationTestBase() {

  private val loggedInUser = "SOME_USER"

  @Test
  fun `can get all allocated events by logged in user`() {
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

    val caseDetails = caseDetailsRepository.save(CaseDetailsEntity(crn = storedEventManager.crn, tier = Tier.B2, type = CaseType.COMMUNITY, "", ""))

    workforceAllocationsToDelius.allocationDetailsResponse(listOf(AllocationDetailIntegration(storedEventManager.crn, storedEventManager.staffCode)))

    webTestClient.get()
      .uri(
        "/allocation/events/me?since=${thirtyDaysInPast()}",
      )
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_READ", loggedInUser))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.cases[?(@.crn == '${storedEventManager.crn}')].name.forename")
      .isEqualTo("Dylan")
      .jsonPath("$.cases[?(@.crn == '${storedEventManager.crn}')].name.surname")
      .isEqualTo("Armstrong")
      .jsonPath("$.cases[?(@.crn == '${storedEventManager.crn}')].name.combinedName")
      .isEqualTo("Dylan Adam Armstrong")
      .jsonPath("$.cases[?(@.crn == '${storedEventManager.crn}')].staff.name.combinedName")
      .isEqualTo("Sally Smith")
      .jsonPath("$.cases[?(@.crn == '${storedEventManager.crn}')].tier")
      .isEqualTo(caseDetails.tier.name)
      .jsonPath("$.cases[?(@.crn == '${storedEventManager.crn}')].allocatedOn")
      .exists()
  }

  @Test
  fun `only get cases since`() {
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

    workforceAllocationsToDelius.allocationDetailsResponse(listOf(AllocationDetailIntegration(oldEventManager.crn, oldEventManager.staffCode)))

    webTestClient.get()
      .uri(
        "/allocation/events/me?since=${thirtyDaysInPast()}",
      )
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_READ", loggedInUser))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.cases[?(@.crn == '${oldEventManager.crn}')]")
      .doesNotExist()
  }

  @Test
  fun `only retrieve active event managers`() {
    val inactiveEventManager = eventManagerRepository.save(
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
        "/allocation/events/me?since=${thirtyDaysInPast()}",
      )
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_READ", loggedInUser))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.cases[?(@.crn == '${inactiveEventManager.crn}')]")
      .doesNotExist()
  }

  @Test
  fun `don't return allocated event managers who have no Delius details`() {
    val noDeliusDetailsEventManager = eventManagerRepository.save(
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

    workforceAllocationsToDelius.allocationDetailsResponse(emptyList())

    caseDetailsRepository.save(CaseDetailsEntity(crn = noDeliusDetailsEventManager.crn, tier = Tier.B2, type = CaseType.COMMUNITY, "", ""))

    webTestClient.get()
      .uri(
        "/allocation/events/me?since=${thirtyDaysInPast()}",
      )
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_READ", loggedInUser))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.cases[?(@.crn == '${noDeliusDetailsEventManager.crn}')]")
      .doesNotExist()
  }

  private fun thirtyDaysInPast(): String? =
    ZonedDateTime.now().minusDays(30).truncatedTo(ChronoUnit.DAYS).withZoneSameInstant(ZoneOffset.UTC).format(
      DateTimeFormatter.ISO_OFFSET_DATE_TIME,
    )
}
