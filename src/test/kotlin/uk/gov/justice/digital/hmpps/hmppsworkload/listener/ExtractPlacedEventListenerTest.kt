package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.service.reduction.UpdateReductionService

class ExtractPlacedEventListenerTest {

  @MockK
  lateinit var objectMapper: ObjectMapper

  @MockK
  lateinit var updateReductionService: UpdateReductionService

  @InjectMockKs
  lateinit var extractPlacedEventListener: ExtractPlacedEventListener

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `test process extract placed message`() = runBlocking<Unit> {
    val rawMessage = "{\"Message\":{\"Text\":\"Some Message\"},\"MessageId\":\"002\"}"
    every { objectMapper.readValue(rawMessage, SQSMessage::class.java) } returns SQSMessage("\"Text\":\"Some Message\"", "002")
    extractPlacedEventListener.processMessage(rawMessage)
    coVerify { updateReductionService.updateOutOfDateReductionStatus() }
  }
}
