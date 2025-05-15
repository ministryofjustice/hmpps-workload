package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsworkload.service.WorkloadCalculationService
import java.math.BigDecimal

@Component
class WorkloadCalculationEventListener(
  private val objectMapper: ObjectMapper,
  private val workloadCalculationService: WorkloadCalculationService,
  @Qualifier("workforceAllocationsToDeliusApiClient") private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {

  @SqsListener("workloadcalculationqueue", factory = "hmppsQueueContainerFactoryProxy")
  fun processMessage(rawMessage: String) = runBlocking {
    val workloadCalculationEvent = getWorkloadCalculationEvent(rawMessage)
    handleSave(workloadCalculationEvent)
  }

  suspend fun handleSave(event: WorkloadCalculationEvent) {
    val availableHours = event.additionalInformation.availableHours
    val staffIdentifier = StaffIdentifier(
      event.personReference.identifiers.find { it.type == "staffCode" }!!.value,
      event.personReference.identifiers.find { it.type == "teamCode" }!!.value,
    )
    val staffGrade = workforceAllocationsToDeliusApiClient.getOfficerView(staffIdentifier.staffCode).getGrade()
    workloadCalculationService.saveWorkloadCalculation(staffIdentifier, staffGrade, availableHours)
  }

  private fun getWorkloadCalculationEvent(rawMessage: String): WorkloadCalculationEvent {
    val (message, messageId) = objectMapper.readValue(rawMessage, SQSMessage::class.java)
    val queueName = System.getenv("HMPPS_SQS_QUEUES_WORKLOADCALCULATIONQUEUE_QUEUE_NAME") ?: "Queue name not found"
    log.info("Received message from $queueName with messageId:$messageId")
    return objectMapper.readValue(message, WorkloadCalculationEvent::class.java)
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class WorkloadCalculationEvent(
  val additionalInformation: AdditionalInformation,
  val personReference: PersonReference,
)

data class AdditionalInformation(
  val availableHours: BigDecimal,
)
