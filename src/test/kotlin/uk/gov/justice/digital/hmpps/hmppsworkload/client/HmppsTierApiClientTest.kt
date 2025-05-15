package uk.gov.justice.digital.hmpps.hmppsworkload.client

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

class HmppsTierApiClientTest {

  @Test
  fun `test get tier by crn`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body("{\"tierScore\":\"B3\"}")
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = HmppsTierApiClient(webClient).getTierByCrn("X123456")
    assertTrue(result == "B3")
  }

  @Test
  fun `test get tier by crn not found`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.NOT_FOUND)
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = HmppsTierApiClient(webClient).getTierByCrn("X123456")
    assertNull(result)
  }

  @Test
  fun `test tier client throws error`() = runBlocking<Unit> {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    assertThrows(WebClientResponseException::class.java) { runBlocking { HmppsTierApiClient(webClient).getTierByCrn("X123456") } }
  }
}
