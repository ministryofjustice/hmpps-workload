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
import reactor.core.publisher.Mono

class HmppsTierApiClientTest {

  @Test
  fun `test get tier by crn`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body("{\"tierScore\":\"B\",\"provisional\":false}")
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = HmppsTierApiClient(webClient).getTierByCrn("X123456")
    assertTrue(result == TierWithStatus("B", false))
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
    assertThrows(WorkloadFailedDependencyException::class.java) { runBlocking { HmppsTierApiClient(webClient).getTierByCrn("X123456") } }
  }

  @Test
  fun `test get tier by crns`() = runBlocking {
    val exchangeFunction = ExchangeFunction { _ ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body("{\"X123456\": {\"tierScore\":\"A\",\"provisional\":false}, \"X234567\": {\"tierScore\":\"B\",\"provisional\":true}}")
          .build(),
      )
    }

    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = HmppsTierApiClient(webClient).getTierByCrns(listOf("X123456", "X234567"))

    assertTrue(result["X123456"] == TierWithStatus("A", false))
    assertTrue(result["X234567"] == TierWithStatus("B", true))
  }

  @Test
  fun `test get tier by crns throws error`() = runBlocking<Unit> {
    val exchangeFunction = ExchangeFunction { _ ->
      Mono.just(
        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    assertThrows(WorkloadFailedDependencyException::class.java) { runBlocking { HmppsTierApiClient(webClient).getTierByCrns(listOf("X123456", "X234567")) } }
  }

  @Test
  fun `test get tier by crns accepts nulls`() = runBlocking {
    val exchangeFunction = ExchangeFunction { _ ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body("{\"X123456\": {\"tierScore\":\"A\",\"provisional\":false}, \"X234567\": null}")
          .build(),
      )
    }

    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = HmppsTierApiClient(webClient).getTierByCrns(listOf("X123456", "X234567"))

    assertTrue(result["X123456"] == TierWithStatus("A", false))
    assertTrue(result["X234567"] == null)
  }
}
