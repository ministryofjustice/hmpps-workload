package uk.gov.justice.digital.hmpps.hmppsworkload.client

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDateTime

class AssessRisksNeedsApiClientTest {
  @Test
  fun `test get risk summary`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body("{\"overallRiskLevel\":\"HIGH\"}")
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRiskSummary("X123456")
    assertTrue(result?.overallRiskLevel == "HIGH")
  }

  @Test
  fun `test get risk summary not found`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.NOT_FOUND)
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRiskSummary("X123456")
    assertNull(result)
  }

  @Test
  fun `test get risk summary error`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRiskSummary("X123456")
    assertNull(result)
  }

  @Test
  fun `test get risk predictors`() = runBlocking {
    val responseBody = """
    [
      {
        "rsrPercentageScore": 0.85,
        "rsrScoreLevel": "High",
        "completedDate": "2025-04-30T15:20:00"
      },
      {
        "rsrPercentageScore": 0.45,
        "rsrScoreLevel": "Low",
        "completedDate": "2025-04-29T09:10:00"
      }
    ]
    """.trimIndent()
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(responseBody)
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRiskPredictors("X123456")
    assertTrue(result.size == 2)
    assertTrue(result.first().rsrPercentageScore == BigDecimal.valueOf(0.85))
    assertTrue(result.first().completedDate == LocalDateTime.parse("2025-04-30T15:20:00"))
  }

  @Test
  fun `test get risk predictors not found`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.NOT_FOUND)
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRiskPredictors("X123456")
    assertTrue(result.isEmpty())
  }

  @Test
  fun `test get risk predictors error`() = runBlocking {
    val exchangeFunction = ExchangeFunction { request ->
      Mono.just(
        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
          .build(),
      )
    }
    val webClient = WebClient.builder().exchangeFunction(exchangeFunction).build()
    val result = AssessRisksNeedsApiClient(webClient).getRiskPredictors("X123456")
    assertTrue(result.isEmpty())
  }
}
