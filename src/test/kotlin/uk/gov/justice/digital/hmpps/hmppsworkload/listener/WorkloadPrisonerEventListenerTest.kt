package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReferenceType
import uk.gov.justice.digital.hmpps.hmppsworkload.service.SaveCaseDetailsService

class WorkloadPrisonerEventListenerTest {
  @MockK
  lateinit var objectMapper: ObjectMapper

  @MockK(relaxed = true)
  lateinit var saveCaseDetailsService: SaveCaseDetailsService


  @InjectMockKs
  lateinit var prisonerEventListener: WorkloadPrisonerEventListener

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `test process prisoner event message`() {
    val rawMessage = """
    {
      "messageID": "002",
      "Message": {
        "personReference": {
          "identifiers": [
            {
              "type": "NOMS",
              "value": "N123456"
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
            "type": "NOMS",
            "value": "N123456"
          }
        ]
      }
    }
    """.trimIndent()
    every { objectMapper.readValue(rawMessage, SQSMessage::class.java) } returns SQSMessage(message, "002")
    every { objectMapper.readValue(message, WorkloadPrisonerEvent::class.java) } returns WorkloadPrisonerEvent(
      PersonReference(listOf(PersonReferenceType("NOMS", "N123456")))
    )

    prisonerEventListener.processMessage(rawMessage)

    coVerify { saveCaseDetailsService.saveByNoms("N123456") }

    confirmVerified(saveCaseDetailsService)
  }

}