package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsworkload.service.NotificationEmail
import uk.gov.justice.digital.hmpps.hmppsworkload.service.NotificationService
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
import uk.gov.service.notify.SendEmailResponse
import java.util.UUID

class NotificationListenerTest {
  @MockK
  lateinit var notificationClient: NotificationClient

  @MockK
  lateinit var objectMapper: ObjectMapper

  @InjectMockKs
  lateinit var notificationListener: NotificationListener

  @BeforeEach
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
  }

  @Test
  fun `test process notification message`() = runBlocking<Unit> {
    var rawMessage = "{\"Message\":{\"Text\":\"Some Message\"},\"MessageId\":\"002\"}"
    var templateId = UUID.randomUUID().toString()
    var referenceId = UUID.randomUUID().toString()
    var emailParameters = mapOf("george" to "mildred")
    var sendEmailResponse = SendEmailResponse(
      """
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "reference": "ref-1234",
      "content": {
        "body": "Hello, this is a test email body.",
        "from_email": "no-reply@example.com",
        "subject": "Test Email Subject"
      },
      "template": {
        "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        "version": 3,
        "uri": "https://api.service.gov.uk/templates/f47ac10b-58cc-4372-a567-0e02b2c3d479"
      },
      "one_click_unsubscribe_url": "https://unsubscribe.example.com/one-click/550e8400-e29b-41d4-a716-446655440000"
    }
      """.trimIndent(),
    )
    var notificationMessage = NotificationEmail(setOf("example@example.com"), templateId, referenceId, emailParameters)
    coEvery { objectMapper.readValue(rawMessage, NotificationEmail::class.java) } returns notificationMessage
    coEvery { notificationClient.sendEmail(templateId, "example@example.com", emailParameters, referenceId) } returns sendEmailResponse
    notificationListener.processMessage(rawMessage, "002")

    coVerify { notificationClient.sendEmail(templateId, "example@example.com", emailParameters, referenceId) }
    coVerify { objectMapper.readValue(rawMessage, NotificationEmail::class.java) }
  }

  @Test
  fun `test process notification message with retries`() = runBlocking<Unit> {
    var rawMessage = "{\"Message\":{\"Text\":\"Some Message\"},\"MessageId\":\"002\"}"
    var templateId = UUID.randomUUID().toString()
    var referenceId = UUID.randomUUID().toString()
    var emailParameters = mapOf("george" to "mildred")
    var sendEmailResponse = SendEmailResponse(
      """
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "reference": "ref-1234",
      "content": {
        "body": "Hello, this is a test email body.",
        "from_email": "no-reply@example.com",
        "subject": "Test Email Subject"
      },
      "template": {
        "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        "version": 3,
        "uri": "https://api.service.gov.uk/templates/f47ac10b-58cc-4372-a567-0e02b2c3d479"
      },
      "one_click_unsubscribe_url": "https://unsubscribe.example.com/one-click/550e8400-e29b-41d4-a716-446655440000"
    }
      """.trimIndent(),
    )
    var notificationMessage = NotificationEmail(setOf("example@example.com"), templateId, referenceId, emailParameters)
    coEvery { objectMapper.readValue(rawMessage, NotificationEmail::class.java) } returns notificationMessage
    coEvery { notificationClient.sendEmail(templateId, "example@example.com", emailParameters, referenceId) } throws NotificationClientException("Failed to send email")
    val exception = assertThrows<NotificationService.NotificationInvalidSenderException> { notificationListener.processMessage(rawMessage, "002") }

    assertTrue(exception.message.equals("Unable to deliver to recipient example@example.com"))
  }
}
