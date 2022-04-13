package uk.gov.justice.digital.hmpps.hmppsworkload.service

import com.amazonaws.services.sns.model.MessageAttributeValue
import com.amazonaws.services.sns.model.PublishRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.HmppsMessage
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.HmppsPersonAllocationMessage
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReferenceType
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingTopicException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
@ConditionalOnProperty("hmpps.sqs.topics.hmppsallocationcompletetopic.arn")
class SqsSuccessUpdater(
  val hmppsQueueService: HmppsQueueService,
  val objectMapper: ObjectMapper,
  @Value("\${ingress.url}") private val ingressUrl: String,
  @Value("\${person.manager.getByIdPath}") private val personManagerLookupPath: String
) : SuccessUpdater {

  private val allocationCompleteTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsallocationcompletetopic")
      ?: throw MissingTopicException("hmppsallocationcompletetopic not found")
  }

  override fun updatePerson(crn: String, allocationId: UUID, timeUpdated: ZonedDateTime) {
    val hmppsPersonEvent = HmppsMessage(
      "PERSON_MANAGER_ALLOCATED", 1, "Person allocated event", UriComponentsBuilder.newInstance().scheme("https").host(ingressUrl).path(personManagerLookupPath).buildAndExpand(allocationId).toUriString(),
      timeUpdated.format(
        DateTimeFormatter.ISO_OFFSET_DATE_TIME
      ),
      HmppsPersonAllocationMessage(allocationId),
      PersonReference(listOf(PersonReferenceType("CRN", crn)))
    )
    allocationCompleteTopic.snsClient.publish(
      PublishRequest(allocationCompleteTopic.arn, objectMapper.writeValueAsString(hmppsPersonEvent))
        .withMessageAttributes(mapOf("eventType" to MessageAttributeValue().withDataType("String").withStringValue(hmppsPersonEvent.eventType)))
    ).also {
      log.info("Published event {} to topic for CRN {} and id {}", hmppsPersonEvent.eventType, crn, allocationId)
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
