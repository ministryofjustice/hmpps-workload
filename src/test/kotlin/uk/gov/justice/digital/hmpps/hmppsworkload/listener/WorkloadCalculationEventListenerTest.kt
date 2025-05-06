package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReferenceType
import uk.gov.justice.digital.hmpps.hmppsworkload.service.WorkloadCalculationService
import java.math.BigDecimal

class WorkloadCalculationEventListenerTest {
  @MockK(relaxed = true)
  private lateinit var workloadCalculationService: WorkloadCalculationService

  @MockK
  private lateinit var objectMapper: ObjectMapper

  @MockK
  private lateinit var workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient

  @InjectMockKs
  private lateinit var workloadCalculationEventListener: WorkloadCalculationEventListener

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `test process workload calculation message`() = runBlocking {
    // given
    val rawMessage = """
      {
        "messageID": "002",
        "Message": {
          "additionalInformation": {
            "availableHours": 37.5
          },
          "personReference": {
            "identifiers": [
              { "type": "CRN",       "value": "X999999" },
              { "type": "staffCode", "value": "SM00234" },
              { "type": "teamCode",  "value": "T1" }
            ]
          }
        }
      }
    """.trimIndent()

    val innerMessage = """
      {
        "additionalInformation": {
          "availableHours": 37.5
        },
        "personReference": {
          "identifiers": [
            { "type": "CRN",       "value": "X999999" },
            { "type": "staffCode", "value": "SM00234" },
            { "type": "teamCode",  "value": "T1" }
          ]
        }
      }
    """.trimIndent()

    every {
      objectMapper.readValue(rawMessage, SQSMessage::class.java)
    } returns SQSMessage(innerMessage, "002")

    every {
      objectMapper.readValue(innerMessage, WorkloadCalculationEvent::class.java)
    } returns WorkloadCalculationEvent(
      AdditionalInformation(BigDecimal("37.5")),
      PersonReference(
        listOf(
          PersonReferenceType("CRN", "X999999"),
          PersonReferenceType("staffCode", "SM00234"),
          PersonReferenceType("teamCode", "T1"),
        ),
      ),
    )

    coEvery {
      workforceAllocationsToDeliusApiClient.getOfficerView("SM00234").getGrade()
    } returns "PO"

    workloadCalculationEventListener.processMessage(rawMessage)

    verify {
      workloadCalculationService.saveWorkloadCalculation(
        StaffIdentifier("SM00234", "T1"),
        "PO",
        BigDecimal("37.5"),
      )
    }
    confirmVerified(workloadCalculationService)
  }
}
