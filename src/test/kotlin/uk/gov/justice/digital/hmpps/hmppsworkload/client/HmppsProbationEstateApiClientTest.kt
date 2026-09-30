package uk.gov.justice.digital.hmpps.hmppsworkload.client

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.TeamOverview

class HmppsProbationEstateApiClientTest {
  @Test
  fun `test get teams`() = runBlocking {
    val exchangeFunction = ExchangeFunction { _ ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body("[{\"code\":\"teamA\",\"name\": \"Team A\"},{\"code\": \"teamB\",\"name\": \"Team B\"}]")
          .build(),
      )
    }

    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = HmppsProbationEstateApiClient(webClient).getTeams(listOf("teamA", "teamB"))

    assertEquals(listOf(TeamOverview("teamA", "Team A"), TeamOverview("teamB", "Team B")), result)
  }

  @Test
  fun `test get teams throws error`() = runBlocking<Unit> {
    val exchangeFunction = ExchangeFunction { _ ->
      Mono.just(
        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
          .build(),
      )
    }

    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()

    assertThrows(WorkloadFailedDependencyException::class.java) { runBlocking { HmppsProbationEstateApiClient(webClient).getTeams(listOf("teamA", "teamB")) } }
  }
}
