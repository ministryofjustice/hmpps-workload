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
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses.riskPredictorResponseV1
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses.riskPredictorResponseV2
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
  fun `test get risk predictors V1`() = runBlocking {
    val responseBody = riskPredictorResponseV1()
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
    assertTrue(result.size == 1)
    assertTrue(result.first().getRSRPercentageScore() == BigDecimal.valueOf(8.5))
    assertTrue(result.first().completedDate == LocalDateTime.parse("2025-10-23T03:02:59"))
  }

  @Test
  fun `test get risk predictors V2`() = runBlocking {
    val responseBody = riskPredictorResponseV2()
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
    assertTrue(result.size == 1)
    assertTrue(result.first().getRSRPercentageScore() == BigDecimal.valueOf(10))
    assertTrue(result.first().completedDate == LocalDateTime.parse("2025-10-23T03:02:59"))
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
