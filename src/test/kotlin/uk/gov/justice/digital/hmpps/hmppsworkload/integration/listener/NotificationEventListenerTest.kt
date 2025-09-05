package uk.gov.justice.digital.hmpps.hmppsworkload.integration.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.listener.NotificationListener
import uk.gov.justice.digital.hmpps.hmppsworkload.service.NotificationEmail
import uk.gov.service.notify.NotificationClient
import java.util.*

class NotificationEventListenerTest : IntegrationTestBase() {

  private val notificationClient = mockk<NotificationClient>()
  private val meterRegistry = mockk<MeterRegistry>()
  override var objectMapper = ObjectMapper()
  var cut = NotificationListener(notificationClient, objectMapper, meterRegistry)

  @Test
  fun `must listen to notification event`() {
    placeMessageOnNotificationQueue()
    noMessagesOnNotificationQueue()
    noMessagesOnNotificationQueueDLQ()
  }

  @Test
  fun `exception (x 3 retries) should put message on DLQ`() {
    every { notificationClient.sendEmail(any(), any(), any(), any()) } throws RuntimeException("Failed to notify")
    placeMessageOnNotificationQueue()
    messagesOnNotificationQueueDLQ()
  }

  private fun placeMessageOnNotificationQueue() {
    var notificationEmail = NotificationEmail(setOf("gary@fred.com", "harry.potter@warner.com"), UUID.randomUUID().toString(), "test", mapOf("george" to "mildred"))
    var messageBody = objectMapper.writeValueAsString(notificationEmail)
    notificationSqsClient.sendMessage(
      SendMessageRequest.builder().queueUrl(notificationQueue.queueUrl).messageBody(messageBody).build(),
    ).get()
  }
}
