package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsworkload.service.SaveCaseDetailsService

@Component
class OffenderEventListener(
  private val objectMapper: ObjectMapper,
  private val saveCaseDetailsService: SaveCaseDetailsService,
) {

  @SqsListener("hmppsoffenderqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String) {
    val (crn) = getCase(rawMessage)
    log.info("Processing message on offender queue for crn $crn")
    CoroutineScope(Dispatchers.Default).future {
      saveCaseDetailsService.saveByCrn(crn)
    }.get()
  }

  private fun getCase(rawMessage: String): HmppsOffenderEvent {
    val (message, messageId) = objectMapper.readValue(rawMessage, SQSMessage::class.java)
    val queueName = System.getenv("HMPPS_SQS_QUEUES_HMPPSOFFENDERQUEUE_QUEUE_NAME") ?: "Queue name not found"
    log.info("Received message from $queueName with messageId:$messageId")
    return objectMapper.readValue(message, HmppsOffenderEvent::class.java)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class HmppsOffenderEvent(
  val crn: String,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SQSMessage(
  @JsonProperty("Message") val message: String?,
  @JsonProperty("MessageId") val messageId: String?,
)
