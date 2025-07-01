package uk.gov.justice.digital.hmpps.hmppsworkload.client

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import java.math.BigInteger
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

class WorkforceAllocationsToDeliusApiClientTest {

  @Test
  fun `test choose practitioners`() = runBlocking<Unit> {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(ClientResponses.deliusResponseChoosePractitioners())
          .build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .choosePractitioners("X999999", listOf("teamA", "teamB", "teamC"))

    assertEquals("X999999", result?.crn)
    assertEquals(2, result?.teams?.size)
    assertEquals("CURRENTLY_MANAGED", result?.probationStatus?.status)
  }

  @Test
  fun `test choose practitioners not found`() = runBlocking<Unit> {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.NOT_FOUND).build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .choosePractitioners("X999999", listOf("teamA", "teamB", "teamC"))

    assertNull(result)
  }

  @Test
  @Suppress("SwallowedException")
  fun `test choose practitioners error`() = runBlocking<Unit> {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    assertThrows<WorkloadFailedDependencyException> {
      runBlocking {
        WorkforceAllocationsToDeliusApiClient(webClient)
          .choosePractitioners("X999999", listOf("teamA", "teamB", "teamC"))
      }
    }
  }

  @Test
  fun `test choose practitioners no crn`() = runBlocking {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(ClientResponses.deliusResponseChoosePractitionersNoCRN())
          .build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .choosePractitioners(listOf("teamA", "teamB"))

    assertEquals("", result?.crn)
    assertTrue(result?.probationStatus?.status.isNullOrEmpty())
    assertTrue(result?.teams?.isNotEmpty() == true)
  }

  @Test
  fun `test choose practitioners no crn not found`() = runBlocking {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.NOT_FOUND).build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .choosePractitioners(listOf("teamA", "teamB"))

    assertNull(result)
  }

  @Test
  @Suppress("SwallowedException")
  fun `test choose practitioners no crn error`() = runBlocking<Unit> {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    assertThrows<WorkloadFailedDependencyException> {
      runBlocking {
        WorkforceAllocationsToDeliusApiClient(webClient)
          .choosePractitioners(listOf("teamA", "teamB"))
      }
    }
  }

  @Test
  fun `test get person (by crn)`() = runBlocking {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(ClientResponses.deliusResponseGetPersonByCRN())
          .build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .getPersonByCrn("X999999")

    assertEquals("X999999", result?.crn)
    assertEquals("John", result?.name?.forename)
    assertEquals(CaseType.LICENSE, result?.type)
  }

  @Test
  fun `test get person (by crn) not found`() = runBlocking {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.NOT_FOUND).build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .getPersonByCrn("X999999")

    assertEquals("X999999", result?.crn)
    assertEquals("Unknown", result?.name?.forename)
    assertEquals(CaseType.UNKNOWN, result?.type)
  }

  @Test
  @Suppress("SwallowedException")
  fun `test get person (by crn) error`() = runBlocking<Unit> {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    assertThrows<WorkloadFailedDependencyException> {
      runBlocking {
        WorkforceAllocationsToDeliusApiClient(webClient)
          .getPersonByCrn("X999999")
      }
    }
  }

  @Test
  fun `test get officer view`() = runBlocking {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(ClientResponses.deliusResponseGetOfficerView())
          .build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .getOfficerView("SM00234")

    assertEquals("John", result.name.forename)
    assertEquals("Smith", result.name.surname)
    assertEquals("PSO", result.getGrade())
    assertEquals(BigInteger.valueOf(7), result.paroleReportsToCompleteInNext4Weeks)
  }

  @Test
  fun `test get impact`() = runBlocking {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(ClientResponses.deliusResponseGetImpact())
          .build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .impact("X999999", "SM00234")

    assertEquals("X999999", result.crn)
    assertEquals("PSO", result.staff.getGrade())
    assertEquals("Smith", result.name.surname)
  }

  @Test
  fun `test allocation complete details`() = runBlocking {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(ClientResponses.deliusResponseAllocationCompleteDetails())
          .build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .allocationCompleteDetails("X999999", "1", "SM00235")

    assertEquals("X999999", result.crn)
    assertEquals(LocalDate.parse("2025-05-22"), result.initialAppointment?.date)
    assertEquals("LICENSE", result.type)
    assertEquals("SM00234", result.staff?.code)
  }

  @Test
  fun `test staff active cases`() = runBlocking {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(ClientResponses.deliusResponseStaffActiveCases())
          .build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .staffActiveCases("SM00234", listOf("X999999"))

    assertEquals("SM00234", result.code)
    assertEquals("Jones", result.name.surname)
    assertEquals("X999999", result.cases[0].crn)
    assertEquals("LICENSE", result.cases[0].type)
  }

  @Test
  fun `test allocation details`() = runBlocking {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(ClientResponses.deliusResponseAllocationDetails())
          .build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .allocationCompleteDetails("X999999", "1", "")

    assertEquals("X999999", result.crn)
    assertEquals("SMITH", result.name.surname)
    assertEquals(LocalDate.parse("2025-05-02"), result.initialAppointment?.date)
    assertEquals("PO", result.staff?.getGrade())
  }

  @Test
  fun `test post delius allocation details`() = runBlocking {
    val jsonRequest = """
      {
        "cases": [
          {
            "crn": "X999999",
            "staffCode": "SM00234"
          }
        ]
      }
    """.trimIndent()

    var capturedRequest: ClientRequest? = null
    val exchangeFunction = ExchangeFunction { request ->
      capturedRequest = request
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(ClientResponses.deliusResponsePostAllocationDetails())
          .build(),
      )
    }
    val eventManagerEntity = EventManagerEntity(
      1L,
      UUID.randomUUID(),
      "X999999",
      "SM00234",
      "TM1",
      "SDB01",
      ZonedDateTime.now(),
      true,
      1,
      "SM00235",
      "John Smith",
    )
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .allocationDetails(listOf(eventManagerEntity))

    assertEquals("X999999", result.cases[0].crn)
    assertNotNull(capturedRequest?.body())
    assertTrue(capturedRequest?.headers()?.get("Content-Type")?.contains("application/json") == true)
    assertEquals("SM00234", result.cases[0].staff.code)
    assertEquals("example@example.com", result.cases[0].staff.email)
  }

  @Test
  fun `test get delius allowed team info`() = runBlocking {
    val exchangeFunction = ExchangeFunction {
      Mono.just(
        ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body(ClientResponses.deliusResponseGetAllowedTeamInfo())
          .build(),
      )
    }
    val webClient = WebClient.builder()
      .exchangeFunction(exchangeFunction)
      .build()

    val result = WorkforceAllocationsToDeliusApiClient(webClient)
      .getDeliusAllowedTeamInfo("SM00234")

    assertEquals(1, result.teams.size)
    assertEquals("TM1", result.teams.get(0).code)
    assertEquals("Team1", result.teams.get(0).description)
    assertEquals("LAU1", result.teams.get(0).localAdminUnit.code)
    assertEquals("Lau1", result.teams.get(0).localAdminUnit.description)
    assertEquals("PDU1", result.teams.get(0).localAdminUnit.probationDeliveryUnit.code)
    assertEquals("Pdu1", result.teams.get(0).localAdminUnit.probationDeliveryUnit.description)
    assertEquals("REG1", result.teams.get(0).localAdminUnit.probationDeliveryUnit.provider.code)
    assertEquals("Region1", result.teams.get(0).localAdminUnit.probationDeliveryUnit.provider.description)
  }
}
