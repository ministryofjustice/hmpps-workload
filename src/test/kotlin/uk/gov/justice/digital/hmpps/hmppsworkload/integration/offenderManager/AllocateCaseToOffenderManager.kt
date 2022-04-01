package uk.gov.justice.digital.hmpps.hmppsworkload.integration.offenderManager

import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.request.allocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import java.math.BigInteger

class AllocateCaseToOffenderManager : IntegrationTestBase() {

  @Test
  fun `can allocate CRN to Offender`() {
    val staffId = 123456789L
    val crn = "CRN1"
    val staffCode = "OM1"
    staffIdResponse(staffId, staffCode)
    offenderSummaryResponse(crn)
    webTestClient.post()
      .uri("/team/T1/offenderManagers/$staffId/cases")
      .bodyValue(allocateCase(crn))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.id")
      .value(MatchesPattern.matchesPattern("([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})"))

    expectPersonAllocationCompleteMessage(crn)
  }

  @Test
  fun `can allocate an already managed CRN to same staff member`() {
    val staffId = BigInteger.valueOf(123456789L)
    val crn = "CRN1"
    val staffCode = "OM1"
    val teamCode = "T1"
    staffIdResponse(staffId.longValueExact(), staffCode)
    offenderSummaryResponse(crn)
    val storedPersonManager = PersonManagerEntity(crn = crn, staffId = staffId, staffCode = staffCode, teamCode = teamCode, offenderName = "John Doe", createdBy = "USER1")
    personManagerRepository.save(storedPersonManager)
    webTestClient.post()
      .uri("/team/$teamCode/offenderManagers/$staffId/cases")
      .bodyValue(allocateCase(crn))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.id")
      .isEqualTo(storedPersonManager.uuid.toString())
  }

  @Test
  fun `cannot allocate an already managed CRN to different staff member`() {
    val staffId = BigInteger.valueOf(123456789L)
    val crn = "CRN1"
    val staffCode = "OM1"
    val teamCode = "T1"
    staffIdResponse(staffId.longValueExact(), staffCode)
    offenderSummaryResponse(crn)
    personManagerRepository.save(PersonManagerEntity(crn = crn, staffId = BigInteger.ONE, staffCode = "ADIFFERENTCODE", teamCode = teamCode, offenderName = "John Doe", createdBy = "USER1"))
    webTestClient.post()
      .uri("/team/$teamCode/offenderManagers/$staffId/cases")
      .bodyValue(allocateCase(crn))
      .headers {
        it.authToken(roles = listOf("ROLE_MANAGE_A_WORKFORCE_ALLOCATE"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isBadRequest
  }
}