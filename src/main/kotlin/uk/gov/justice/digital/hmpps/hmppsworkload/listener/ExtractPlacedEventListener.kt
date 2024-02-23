package uk.gov.justice.digital.hmpps.hmppsworkload.listener

import io.awspring.cloud.sqs.annotation.SqsListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsworkload.service.reduction.UpdateReductionService

@Component
class ExtractPlacedEventListener(
  private val updateReductionService: UpdateReductionService,
) {

  @SqsListener("hmppsextractplacedqueue", factory = "hmppsQueueContainerFactoryProxy")
  suspend fun processMessage(rawMessage: String) {
    CoroutineScope(Dispatchers.Default).async {
      updateReductionService.updateOutOfDateReductionStatus()
    }.await()
  }
}
