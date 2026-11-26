package uk.gov.justice.digital.hmpps.hmppsworkload.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

class QueueAdminTest : IntegrationTestBase() {

  @Test
  fun `should not allow purge with no auth`() {
    webTestClient.put()
      .uri("/queue-admin/purge-queue/hmpps_allocation_complete_event_dlq")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `should allow purge with correct auth`() {
    webTestClient.put()
      .uri("/queue-admin/purge-queue/hmpps_allocation_complete_event_dlq")
      .headers { it.authToken(roles = listOf("ROLE_QUEUE_WORKLOAD_ADMIN")) }
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
  }
}
