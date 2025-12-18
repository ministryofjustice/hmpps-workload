package uk.gov.justice.digital.hmpps.hmppsworkload.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.HmppsAllocationMessage
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.HmppsMessage
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReferenceType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.StaffAvailableHours
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.ReductionEntity
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val EVENT_TYPE = "eventType"

private const val STRING = "String"

private const val LOG_TEMPLATE = "Published event {} to topic for CRN {} and id {}"

private const val LOG_TEMPLATE_NOTIFICATION = "Published notification event to topic for email: {} and reference id: {}"

@Service
@ConditionalOnProperty("hmpps.sqs.topics.hmppsdomaintopic.arn")
class SqsSuccessPublisher(
  val hmppsQueueService: HmppsQueueService,
  val objectMapper: ObjectMapper,
  @Value("\${ingress.url}") private val ingressUrl: String,
  @Value("\${person.manager.getByIdPath}") private val personManagerLookupPath: String,
  @Value("\${event.manager.getByIdPath}") private val eventManagerLookupPath: String,
  @Value("\${requirement.manager.getByIdPath}") private val requirementManagerLookupPath: String,
) {

  private val domainTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsdomaintopic")
      ?: throw MissingTopicException("hmppsdomaintopic not found")
  }

  private val hmppsReductionsCompletedQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsreductionscompletedqueue")
      ?: throw MissingQueueException("HmppsQueue hmppsreductionsCompletedqueue not found")
  }

  private val hmppsAuditQueue by lazy { hmppsQueueService.findByQueueId("hmppsauditqueue") ?: throw MissingQueueException("HmppsQueue hmppsauditqueue not found") }

  fun updatePerson(crn: String, allocationId: UUID, timeUpdated: ZonedDateTime) {
    val hmppsPersonEvent = HmppsMessage(
      "person.community.manager.allocated",
      1,
      "Person allocated event",
      generateDetailsUri(personManagerLookupPath, allocationId),
      timeUpdated.format(
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
      ),
      HmppsAllocationMessage(allocationId),
      PersonReference(listOf(PersonReferenceType("CRN", crn))),
    )
    domainTopic.snsClient.publish(
      PublishRequest.builder()
        .topicArn(domainTopic.arn)
        .message(objectMapper.writeValueAsString(hmppsPersonEvent))
        .messageAttributes(
          mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsPersonEvent.eventType).build()),
        ).build(),
    ).also {
      log.info(LOG_TEMPLATE, hmppsPersonEvent.eventType, crn, allocationId)
    }
  }

  private fun generateDetailsUri(path: String, allocationId: UUID): String = UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(path).buildAndExpand(allocationId).toUriString()

  fun updateEvent(crn: String, allocationId: UUID, timeUpdated: ZonedDateTime) {
    val hmppsEventAllocatedEvent = HmppsMessage(
      "event.manager.allocated",
      1,
      "Event allocated event",
      generateDetailsUri(eventManagerLookupPath, allocationId),
      timeUpdated.format(
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
      ),
      HmppsAllocationMessage(allocationId),
      PersonReference(listOf(PersonReferenceType("CRN", crn))),
    )
    domainTopic.snsClient.publish(
      PublishRequest.builder()
        .topicArn(domainTopic.arn)
        .message(objectMapper.writeValueAsString(hmppsEventAllocatedEvent))
        .messageAttributes(
          mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsEventAllocatedEvent.eventType).build()),
        ).build(),
    ).also {
      log.info(LOG_TEMPLATE, hmppsEventAllocatedEvent.eventType, crn, allocationId)
    }
  }

  fun updateRequirement(crn: String, allocationId: UUID, timeUpdated: ZonedDateTime) {
    val hmppsRequirementAllocatedEvent = HmppsMessage(
      "requirement.manager.allocated",
      1,
      "Requirement allocated event",
      generateDetailsUri(requirementManagerLookupPath, allocationId),
      timeUpdated.format(
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
      ),
      HmppsAllocationMessage(allocationId),
      PersonReference(listOf(PersonReferenceType("CRN", crn))),
    )
    domainTopic.snsClient.publish(
      PublishRequest.builder()
        .topicArn(domainTopic.arn)
        .message(objectMapper.writeValueAsString(hmppsRequirementAllocatedEvent))
        .messageAttributes(
          mapOf(EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(hmppsRequirementAllocatedEvent.eventType).build()),
        ).build(),
    ).also {
      log.info(LOG_TEMPLATE, hmppsRequirementAllocatedEvent.eventType, crn, allocationId)
    }
  }

  fun sendNotification(notificationEmail: NotificationEmail) {
    val sendMessage = SendMessageRequest.builder()
      .queueUrl(hmppsQueueService.findByQueueId("hmppsnotificationqueue")?.queueUrl)
      .messageBody(objectMapper.writeValueAsString(notificationEmail))
      .build()
    hmppsQueueService.findByQueueId("hmppsnotificationqueue")?.sqsClient?.sendMessage(sendMessage)
    log.info(LOG_TEMPLATE_NOTIFICATION, notificationEmail.emailTo, notificationEmail.emailReferenceId)
  }

  fun outOfDateReductionsProcessed() {
    val sendMessage =
      SendMessageRequest.builder()
        .queueUrl(hmppsReductionsCompletedQueue.queueUrl)
        .messageBody(getReductionChangeMessage())
        .messageAttributes(
          mapOf(EVENT_TYPE to software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder().dataType(STRING).stringValue("OUT_OF_DATE_REDUCTIONS").build()),
        ).build()

    log.info("publishing event type OUT_OF_DATE_REDUCTIONS")
    hmppsReductionsCompletedQueue.sqsClient.sendMessage(sendMessage)
  }

  fun auditAllocation(
    crn: String,
    eventNumber: Int?,
    loggedInUser: String,
    requirementIds: List<BigInteger>,
  ) {
    val auditData = AuditData(
      crn,
      eventNumber,
      requirementIds,
    )

    val sendMessage = SendMessageRequest.builder()
      .queueUrl(hmppsAuditQueue.queueUrl)
      .messageBody(
        objectMapper.writeValueAsString(
          AuditMessage(operationId = UUID.randomUUID().toString(), who = loggedInUser, details = objectMapper.writeValueAsString(auditData)),
        ),
      )
      .build()

    hmppsAuditQueue.sqsClient.sendMessage(sendMessage)
  }
  fun auditReduction(reductionEntity: ReductionEntity, reductionStatus: String) {
    val reductionsAuditData = ReductionsAuditData(
      reductionEntity.workloadOwner.offenderManager.code,
      reductionEntity.id!!,
    )

    val sendMessage = SendMessageRequest.builder()
      .queueUrl(hmppsAuditQueue.queueUrl)
      .messageBody(
        objectMapper.writeValueAsString(
          AuditMessage(operationId = UUID.randomUUID().toString(), who = "system_user", details = objectMapper.writeValueAsString(reductionsAuditData), what = reductionStatus),
        ),
      )
      .build()
    hmppsAuditQueue.sqsClient.sendMessage(sendMessage)
  }
  fun staffAvailableHoursChange(staffCode: String, teamCode: String, availableHours: BigDecimal) {
    val staffAvailableHoursChangeMessage = HmppsMessage(
      "staff.available.hours.changed",
      1,
      "Staff Available hours changed",
      "",
      ZonedDateTime.now().format(
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
      ),
      StaffAvailableHours(availableHours),
      PersonReference(listOf(PersonReferenceType("staffCode", staffCode), PersonReferenceType("teamCode", teamCode))),
    )
    domainTopic.snsClient.publish(
      PublishRequest.builder()
        .topicArn(domainTopic.arn)
        .message(objectMapper.writeValueAsString(staffAvailableHoursChangeMessage))
        .messageAttributes(
          mapOf(
            EVENT_TYPE to MessageAttributeValue.builder().dataType(STRING).stringValue(staffAvailableHoursChangeMessage.eventType).build(),
          ),
        )
        .build(),
    ).also {
      log.info("staff available hours changed message for {} in team {}", staffCode, teamCode)
    }
  }

  private fun getReductionChangeMessage(): String = objectMapper.writeValueAsString(
    HmppsMessage(
      "OUT_OF_DATE_REDUCTIONS",
      1,
      "Out of date reduction status are now correct",
      "",
      ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      objectMapper.createObjectNode(),
      PersonReference(emptyList()),
    ),
  )

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}

data class AuditMessage(val operationId: String, val what: String = "CASE_ALLOCATED", val `when`: Instant = Instant.now(), val who: String, val service: String = "hmpps-workload", val details: String)

data class AuditData(
  val crn: String,
  val eventNumber: Int?,
  val requirementIds: List<BigInteger>,
)

data class ReductionsAuditData(
  val staffCode: String,
  val reductionId: Long,
)
