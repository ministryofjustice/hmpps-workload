package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsworkload.service.NotificationEmail
import uk.gov.service.notify.NotificationClientApi
import uk.gov.service.notify.NotificationClientException
import uk.gov.service.notify.SendEmailResponse

private const val MAX_RETRIES = 3
private const val OFFICER = "officer_name"
private const val CRN = "crn"
private const val FAILED_ALLOCATION_COUNTER = "failed_allocation_notification"

@Component
class NotificationListener(
  private val notificationClient: NotificationClientApi,
  private val objectMapper: ObjectMapper,
  private val meterRegistry: MeterRegistry,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("hmppsnotificationqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String, @Header("id") messageId: String) {
    log.info("Processing message on notification queue for messageId: $messageId")
    val notification = getNotification(rawMessage)
    notification.emailTo.map { email ->
      log.info("Sending email to $email")
      log.info("Email template: ${notification.emailTemplate}")
      try {
        handleError(email) {
          notificationClient.sendEmail(
            notification.emailTemplate,
            email,
            notification.emailParameters,
            notification.emailReferenceId,
          )
        }
      } catch (notificationException: NotificationClientException) {
        val crn = notification.emailParameters.getOrDefault(CRN, "UNKNOWN CRN")
        val officer = notification.emailParameters.getOrDefault(OFFICER, "Unknown Officer")
        val emailTo = notification.emailParameters.getOrDefault("practitioner_email", "Unknown sender")
        val allocatingEmail = notification.emailParameters.getOrDefault("allocating_email", "Unknown allocating email")
        meterRegistry.counter(FAILED_ALLOCATION_COUNTER, "type", "email not send").increment()
        log.warn("Failed to send allocation email_to {} email_from {} from_officer {}: for_crn {} : {}", emailTo, allocatingEmail, officer, crn, notificationException.message)
        throw notificationException
      }
    }
  }

  private fun handleError(emailRecipient: String, wrappedApiCall: () -> SendEmailResponse): SendEmailResponse {
    var attempt = 0
    while (true) {
      try {
        val sendEmailResponse = wrappedApiCall.invoke()
        log.info("Allocation Email sent to {} for referenceId: {}", emailRecipient, sendEmailResponse.reference)
        return sendEmailResponse
      } catch (notificationException: NotificationClientException) {
        if (notificationException.httpResult == 500 && attempt < MAX_RETRIES) {
          attempt++
          log.warn("Retrying notify send for {} ", notificationException.message)
          continue
        }
        if (notificationException.httpResult == 400) {
          log.warn("Failed to  send for {} ", notificationException.message)
          log.error("Unable to deliver to recipient $emailRecipient (Invalid Sender)")
        }
        throw notificationException
      }
    }
  }

  private fun getNotification(rawMessage: String): NotificationEmail = objectMapper.readValue(rawMessage, NotificationEmail::class.java)
}
