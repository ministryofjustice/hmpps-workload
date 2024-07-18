package uk.gov.justice.digital.hmpps.hmppsworkload.integration.contactLogging

import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.ContactLoggingMessage
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase

class LogContact : IntegrationTestBase() {

  val contactLoggingMessage = ContactLoggingMessage("X1234321", true, true)

  @Test
  fun `will log valid data`() {
    webTestClient.post()
      .uri("/allocations/contact/logging")
      .body(BodyInserters.fromValue(contactLoggingMessage))
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT"))
      }
      .exchange()
      .expectStatus()
      .isOk
  }
}
