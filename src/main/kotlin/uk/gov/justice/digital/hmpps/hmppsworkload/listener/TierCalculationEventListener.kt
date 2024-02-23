package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppsworkload.service.SaveCaseDetailsService

@Component
class TierCalculationEventListener(
  private val objectMapper: ObjectMapper,
  private val saveCaseDetailsService: SaveCaseDetailsService,
) {

  @SqsListener("tiercalcqueue", factory = "hmppsQueueContainerFactoryProxy")
  suspend fun processMessage(rawMessage: String) {
    val calculationEventData = readMessage(rawMessage)
    CoroutineScope(Dispatchers.Default).async {
      saveCaseDetailsService.saveByCrn(crnFrom(calculationEventData))
    }.await()
  }

  private fun readMessage(wrapper: String?): CalculationEventData {
    val (message) = objectMapper.readValue(wrapper, Message::class.java)
    return objectMapper.readValue(message, CalculationEventData::class.java)
  }

  private fun crnFrom(calculationEventData: CalculationEventData) =
    calculationEventData.personReference.identifiers.first { it.type == "CRN" }.value

  data class CalculationEventData(val personReference: PersonReference)

  data class Message(@JsonProperty("Message") val message: String)
}
