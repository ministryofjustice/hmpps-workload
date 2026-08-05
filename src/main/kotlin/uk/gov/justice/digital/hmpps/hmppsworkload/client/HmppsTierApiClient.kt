package uk.gov.justice.digital.hmpps.hmppsworkload.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import org.springframework.web.reactive.function.client.createExceptionAndAwait

@Suppress("SwallowedException")
class HmppsTierApiClient(private val webClient: WebClient) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  suspend fun getTierByCrn(crn: String): String? {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/crn/{crn}/tier", crn)
          .awaitExchangeOrNull { response ->
            when {
              response.statusCode() == HttpStatus.OK -> {
                response.awaitBody<TierDto>().tierScore
              }
              response.statusCode() == HttpStatus.NOT_FOUND -> {
                null
              }
              response.statusCode().is5xxServerError -> {
                throw WorkloadFailedDependencyException("Tier service failed with ${response.statusCode()}")
              }
              else -> throw response.createExceptionAndAwait()
            }
          }
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    } catch (e: WorkloadFailedDependencyException) {
      log.warn("Tier client failed due to Failed Dependency", e)
      throw WorkloadFailedDependencyException(e.message!!)
    }
  }

  suspend fun getTierByCrns(crns: List<String>): Map<String, String> {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .post()
          .uri("/v3/crns/tier")
          .bodyValue(crns)
          .awaitExchange { response ->
            when {
              response.statusCode() == HttpStatus.OK -> {
                response.awaitBody<Map<String, TierDto>>().mapValues { it.value.tierScore }
              }
              response.statusCode().is5xxServerError -> {
                throw WorkloadFailedDependencyException("Tier service failed with ${response.statusCode()}")
              }
              else -> throw response.createExceptionAndAwait()
            }
          }
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message ?: "Tier service request timed out")
    } catch (e: WorkloadFailedDependencyException) {
      log.warn("Tier client failed due to Failed Dependency", e)
      throw WorkloadFailedDependencyException(e.message ?: "Tier service failed")
    }
  }
}

class WorkloadWebClientTimeoutException(message: String) : RuntimeException(message)
class WorkloadFailedDependencyException(message: String) : RuntimeException(message)

data class TierDto @JsonCreator constructor(
  @JsonProperty("tierScore")
  val tierScore: String,
)
