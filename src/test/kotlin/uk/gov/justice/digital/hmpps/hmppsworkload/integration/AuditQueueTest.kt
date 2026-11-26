package uk.gov.justice.digital.hmpps.hmppsworkload.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.hmppsworkload.service.AuditData
import java.math.BigInteger

class AuditQueueTest : IntegrationTestBase() {
  @Test
  fun `should post audit`() {
    val auditData = AuditData("X123456", 1, listOf(BigInteger.valueOf(1)))
    webTestClient.post()
      .uri("/audit/data")
      .headers { it.authToken(roles = listOf("ROLE_WORKLOAD_READ")) }
      .body(BodyInserters.fromValue(auditData))
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk
  }
}
