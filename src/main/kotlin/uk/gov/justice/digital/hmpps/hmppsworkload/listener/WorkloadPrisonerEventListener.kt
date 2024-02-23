package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsworkload.service.SaveCaseDetailsService

@Component
class WorkloadPrisonerEventListener(
  private val objectMapper: ObjectMapper,
  private val saveCaseDetailsService: SaveCaseDetailsService,
) {

  @SqsListener("workloadprisonerqueue", factory = "hmppsQueueContainerFactoryProxy")
  suspend fun processMessage(rawMessage: String) {
    val nomsNumber = getNomsNumber(rawMessage)
    CoroutineScope(Dispatchers.Default).async {
      saveCaseDetailsService.saveByNoms(nomsNumber)
    }.await()
  }

  private fun getNomsNumber(rawMessage: String): String {
    val (message) = objectMapper.readValue(rawMessage, SQSMessage::class.java)
    val event = objectMapper.readValue(message, WorkloadPrisonerEvent::class.java)
    return event.personReference.identifiers.find { it.type == "NOMS" }!!.value
  }
}

data class WorkloadPrisonerEvent(
  val personReference: PersonReference,
)
