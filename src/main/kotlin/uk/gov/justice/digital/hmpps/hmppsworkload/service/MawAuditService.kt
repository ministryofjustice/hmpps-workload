package uk.gov.justice.digital.hmpps.hmppsworkload.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.util.*

@Service
@ConditionalOnProperty("hmpps.sqs.topics.hmppsdomaintopic.arn")
class MawAuditService(
  val hmppsQueueService: HmppsQueueService,
  val objectMapper: ObjectMapper,
) {

  private val hmppsAuditQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsauditqueue")
      ?: throw MissingQueueException("HmppsQueue hmppsauditqueue not found")
  }

  suspend fun auditData(auditData: Any, loggedInUser: String) {
    val sendMessage = SendMessageRequest.builder()
      .queueUrl(hmppsAuditQueue.queueUrl)
      .messageBody(
        objectMapper.writeValueAsString(
          AuditMessage(operationId = UUID.randomUUID().toString(), what = "Manage a Workforce Audit", who = loggedInUser, details = objectMapper.writeValueAsString(auditData)),
        ),
      )
      .build()

    hmppsAuditQueue.sqsClient.sendMessage(sendMessage)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
