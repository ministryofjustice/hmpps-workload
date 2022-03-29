package uk.gov.justice.digital.hmpps.hmppsworkload.integration.offenderManager

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.request.impactCase

class GetImpactForOffenderManager : IntegrationTestBase() {

  @Test
  fun `can get impact for an offender manager with workload`() {
    val staffId = 123456789L
    val crn = "CRN1"
    val staffCode = "OM1"
    staffIdResponse(staffId, staffCode)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    webTestClient.post()
      .uri("/team/T1/offenderManagers/$staffId/impact")
      .bodyValue(impactCase(crn))
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.forename")
      .isEqualTo("Ben")
      .jsonPath("$.surname")
      .isEqualTo("Doe")
      .jsonPath("$.grade")
      .isEqualTo("PO")
      .jsonPath("$.capacity")
      .isEqualTo(50)
      .jsonPath("$.code")
      .isEqualTo(staffCode)
      .jsonPath("$.potentialCapacity")
      .isEqualTo(55)
  }

  @Test
  fun `can get impact for an offender manager without workload`() {
    val staffId = 123456789L
    val crn = "CRN1"
    val staffCode = "NOWORKLOAD1"
    staffIdResponse(staffId, staffCode)
    tierCalculationResponse(crn)
    singleActiveConvictionResponse(crn)
    webTestClient.post()
      .uri("/team/T1/offenderManagers/$staffId/impact")
      .bodyValue(impactCase(crn))
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT"))
        it.contentType = MediaType.APPLICATION_JSON
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.forename")
      .isEqualTo("Ben")
      .jsonPath("$.surname")
      .isEqualTo("Doe")
      .jsonPath("$.grade")
      .isEqualTo("PO")
      .jsonPath("$.capacity")
      .isEqualTo(0)
      .jsonPath("$.code")
      .isEqualTo(staffCode)
      .jsonPath("$.potentialCapacity")
      .isEqualTo(2)
  }
}
