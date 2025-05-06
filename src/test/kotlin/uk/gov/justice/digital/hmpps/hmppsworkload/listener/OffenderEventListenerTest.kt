package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.service.SaveCaseDetailsService

class OffenderEventListenerTest {

  @MockK
  private lateinit var saveCaseDetailsService: SaveCaseDetailsService

  @MockK
  private lateinit var objectMapper: ObjectMapper

  @InjectMockKs
  lateinit var offenderEventListener: OffenderEventListener

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `test process offender event message`() {
    val rawMessage = "{\"Message\":{\"crn\":\"X999999\"},\"MessageId\":\"002\"}"
    val message = "{\"crn\":\"X999999\"}"
    coEvery { objectMapper.readValue(rawMessage, SQSMessage::class.java) } returns SQSMessage(message, "002")
    coEvery { objectMapper.readValue(message, HmppsOffenderEvent::class.java) } returns HmppsOffenderEvent("X999999")
    offenderEventListener.processMessage(rawMessage)
    coVerify { saveCaseDetailsService.saveByCrn("X999999") }
  }
}
