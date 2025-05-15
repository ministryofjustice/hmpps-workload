package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReferenceType
import uk.gov.justice.digital.hmpps.hmppsworkload.service.SaveCaseDetailsService

class TierCalculationEventListenerTest {

  @MockK
  private lateinit var objectMapper: ObjectMapper

  @MockK
  private lateinit var saveCaseDetailsService: SaveCaseDetailsService

  @InjectMockKs
  private lateinit var tierCalculationEventListener: TierCalculationEventListener

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `test process tier calculation message`() = runBlocking {
    val rawMessage = """
    {
      "messageID": "002",
      "Message": {
        "personReference": {
          "identifiers": [
            {
              "type": "CRN",
              "value": "X999999"
            }
          ]
        }
      }
    }
    """.trimIndent()
    val message = """
    {
      "personReference": {
        "identifiers": [
          {
            "type": "CRN",
            "value": "X999999"
          }
        ]
      }
    }
    """.trimIndent()
    coEvery { objectMapper.readValue(rawMessage, SQSMessage::class.java) } returns SQSMessage(message, "002")
    coEvery {
      objectMapper.readValue(
        message,
        TierCalculationEventListener.CalculationEventData::class.java,
      )
    } returns TierCalculationEventListener.CalculationEventData(
      PersonReference(listOf(PersonReferenceType("CRN", "X999999"))),
    )
    tierCalculationEventListener.processMessage(rawMessage)
    coVerify { saveCaseDetailsService.saveByCrn("X999999") }
  }
}
