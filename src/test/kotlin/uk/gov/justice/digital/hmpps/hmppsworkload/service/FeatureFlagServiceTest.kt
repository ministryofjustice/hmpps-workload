package uk.gov.justice.digital.hmpps.hmppsworkload.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsworkload.client.FeatureFlagClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.FeatureFlagRequest
import uk.gov.justice.digital.hmpps.hmppsworkload.client.FeatureFlagResponse

@ActiveProfiles("test")
class FeatureFlagServiceTest {

  private val featureFlagClient = mockk<FeatureFlagClient>()
  private val service = FeatureFlagService(featureFlagClient)

  @Test
  fun `should return feature flag response from client`() {
    val flagKey = "test-flag"
    val context = mapOf("user" to "test-user")
    val expectedResponse = FeatureFlagResponse(enabled = true)
    val request = FeatureFlagRequest(
      namespace = "ManageAWorkforce",
      entityId = flagKey,
      flagKey = flagKey,
      context = context,
    )
    every { featureFlagClient.getFeatureFlags(request) } returns Mono.just(expectedResponse)

    val result = service.isFeatureEnabled(flagKey, context).block()

    assert(result == expectedResponse)
  }

  @Test
  fun `should return feature flag response from client without context`() {
    val flagKey = "test-flag"
    val expectedResponse = FeatureFlagResponse(enabled = false)
    val request = FeatureFlagRequest(
      namespace = "ManageAWorkforce",
      entityId = flagKey,
      flagKey = flagKey,
      context = null,
    )
    every { featureFlagClient.getFeatureFlags(request) } returns Mono.just(expectedResponse)

    val result = service.isFeatureEnabled(flagKey).block()

    assert(result == expectedResponse)
  }

  @Test
  fun `should evict featureFlags cache`() {
    // Just call the method to ensure it does not throw
    service.evictFeatureFlagsCache()
  }
}
