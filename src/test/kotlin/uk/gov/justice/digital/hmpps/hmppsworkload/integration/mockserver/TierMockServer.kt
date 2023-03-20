package uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.MediaType
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.TierApiExtension.Companion.hmppsTier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses.notFoundTierResponse

class TierApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

  companion object {

    lateinit var hmppsTier: TierMockServer
  }

  override fun beforeAll(context: ExtensionContext?) {
    hmppsTier = TierMockServer()
  }
  override fun beforeEach(context: ExtensionContext?) {
    hmppsTier.reset()
  }
  override fun afterAll(context: ExtensionContext?) {
    hmppsTier.stop()
  }
}
class TierMockServer : ClientAndServer(MOCKSERVER_PORT) {

  companion object {
    private const val MOCKSERVER_PORT = 8082
  }
  fun tierCalculationResponse(crn: String, tier: String = "B3") {
    val request = HttpRequest.request().withPath("/crn/$crn/tier")
    hmppsTier.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody("{\"tierScore\":\"${tier}\"}"),
    )
  }

  fun tierCalculationNotFoundResponse(crn: String) {
    val request = HttpRequest.request().withPath("/crn/$crn/tier")
    hmppsTier.`when`(request, Times.exactly(1)).respond(
      HttpResponse.notFoundResponse().withContentType(MediaType.APPLICATION_JSON).withBody(notFoundTierResponse()),
    )
  }
}
