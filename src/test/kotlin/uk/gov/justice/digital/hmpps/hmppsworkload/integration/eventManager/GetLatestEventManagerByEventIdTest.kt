package uk.gov.justice.digital.hmpps.hmppsworkload.integration.eventManager

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import java.math.BigInteger

class GetLatestEventManagerByEventIdTest : IntegrationTestBase() {

  @Test
  fun `can get case details by crn and event number`() {
    val storedEventManager = EventManagerEntity(crn = "CRN1", staffCode = "OM1", teamCode = "T1", createdBy = "USER1", eventId = BigInteger.valueOf(567891234L), isActive = true, eventNumber = 2)
    eventManagerRepository.save(storedEventManager)

    val caseDetailsEntity = CaseDetailsEntity(crn = storedEventManager.crn, Tier.C2, CaseType.CUSTODY, "Jane", "Doe")
    caseDetailsRepository.save(caseDetailsEntity)

    webTestClient.get()
      .uri("/allocation/person/${storedEventManager.crn}/event/${storedEventManager.eventNumber}/details")
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_READ"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.tier")
      .isEqualTo(caseDetailsEntity.tier.name)
      .jsonPath("$.personOnProbationFirstName")
      .isEqualTo(caseDetailsEntity.firstName)
      .jsonPath("$.personOnProbationSurname")
      .isEqualTo(caseDetailsEntity.surname)
  }

  @Test
  fun `not found returned when getting event manager from uuid which does not exist`() {
    webTestClient.get()
      .uri("/allocation/person/CRN1111/event/6/details")
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_READ"))
      }
      .exchange()
      .expectStatus()
      .isNotFound
  }
}
