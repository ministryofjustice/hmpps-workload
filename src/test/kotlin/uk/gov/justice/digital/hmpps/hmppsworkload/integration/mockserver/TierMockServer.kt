package uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.JsonBody
import org.mockserver.model.MediaType
import org.mockserver.verify.VerificationTimes
import uk.gov.justice.digital.hmpps.hmppsworkload.client.TierDto
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.TierApiExtension.Companion.hmppsTier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses.notFoundTierResponse

class TierApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

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

  fun bulkTierCalculationResponse(crnTiers: Map<String, String>) {
    val response = crnTiers.mapValues { TierDto(it.value) }
    val request = HttpRequest.request().withMethod("POST").withPath("/v3/crns/tier")
    hmppsTier.`when`(request).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(JsonBody.json(response)),
    )
  }

  fun verifyTierCalled(crn: String, times: Int) {
    TierApiExtension.hmppsTier.verify(
      HttpRequest.request()
        .withPath("/crn/$crn/tier"),
      VerificationTimes.exactly(times),
    )
  }
}
