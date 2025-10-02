package uk.gov.justice.digital.hmpps.hmppsworkload.client

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class FeatureFlagClient(private val webClient: WebClient) {

  fun getFeatureFlags(request: FeatureFlagRequest): Mono<FeatureFlagResponse> {
    val evaluationPath = "/ap1/v1/evaluate"
    return webClient.post()
      .uri(evaluationPath)
      .bodyValue(request)
      .header("Content-Type", "application/json ")
      .retrieve()
      .bodyToMono(FeatureFlagResponse::class.java)
  }
}

data class FeatureFlagRequest(
  val namespace: String = "ManageAWorkforce",
  val entityId: String,
  val flagKey: String,
  val context: Map<String, String>,
)

data class FeatureFlagResponse(
  val enabled: Boolean,
)