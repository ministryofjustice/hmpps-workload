package uk.gov.justice.digital.hmpps.hmppsworkload.client

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withTimeout
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskPredictorNew
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskSummary

const val TIMEOUT_VALUE = 3000L
const val OFFICER_VIEW_TIMEOUT_VALUE = 15000L

@Suppress("SwallowedException")
@Component
class AssessRisksNeedsApiClient(@Qualifier("assessRisksNeedsClientUserEnhancedAppScope") private val webClient: WebClient) {

  suspend fun getRiskSummary(crn: String): RiskSummary? {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/risks/crn/{crn}/summary", crn)
          .retrieve()
          .bodyToMono(RiskSummary::class.java)
          .retry(1)
          .onErrorResume {
            Mono.empty()
          }.awaitSingleOrNull()
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getRiskPredictors(crn: String): List<RiskPredictorNew<Any>> {
    val responseType = object : ParameterizedTypeReference<List<RiskPredictorNew<Any>>>() {}
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/risks/predictors/all/crn/{crn}", crn)
          .retrieve()
          .bodyToMono(responseType)
          .retry(1)
          .onErrorResume {
            Mono.just(emptyList())
          }.awaitSingle()
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    }
  }
}
