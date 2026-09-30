package uk.gov.justice.digital.hmpps.hmppsworkload.client

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.TeamOverview
import kotlin.time.Duration.Companion.milliseconds

private const val TIMEOUT_VALUE = 30000L

@Suppress("SwallowedException")
class HmppsProbationEstateApiClient(private val webClient: WebClient) {
  suspend fun getTeams(teamCodes: List<String>): List<TeamOverview> {
    try {
      return withTimeout(TIMEOUT_VALUE.milliseconds) {
        webClient
          .get()
          .uri { uriBuilder -> uriBuilder.path("/team/search").queryParam("codes", teamCodes).build() }
          .awaitExchangeOrNull { response ->
            when (response.statusCode()) {
              HttpStatus.OK -> response.awaitBody<List<TeamOverview>>()
              HttpStatus.INTERNAL_SERVER_ERROR -> throw WorkloadFailedDependencyException("/team/search failed for 500 error")
              else -> {
                log.error(
                  "Unexpected response from probation-estate's team search API. Getting response-status: {}",
                  response.statusCode(),
                )
                null
              }
            }
          }!!
      }
    } catch (e: TimeoutCancellationException) {
      log.warn("/team/search failed for timeout", e)
      throw WorkloadWebClientTimeoutException(e.message!!)
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
