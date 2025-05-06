package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsworkload.service.reduction.UpdateReductionService

@Component
class ExtractPlacedEventListener(
  private val objectMapper: ObjectMapper,
  private val updateReductionService: UpdateReductionService,
) {

  @SqsListener("hmppsextractplacedqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String) {
    val sqsMessage = objectMapper.readValue(rawMessage, SQSMessage::class.java)
    val (message, messageId) = sqsMessage
    val queueName = System.getenv("HMPPS_SQS_QUEUES_HMPPSEXTRACTPLACEDQUEUE_QUEUE_NAME") ?: "Queue name not found"
    log.info("Received message from $queueName with messageId:$message", queueName, messageId)
    CoroutineScope(Dispatchers.Default).future {
      updateReductionService.updateOutOfDateReductionStatus()
    }.get()
  }
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
