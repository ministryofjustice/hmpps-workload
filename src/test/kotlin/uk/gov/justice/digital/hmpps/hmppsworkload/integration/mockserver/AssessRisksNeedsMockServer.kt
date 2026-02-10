package uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.mockserver.integration.ClientAndServer
import org.mockserver.matchers.Times
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.mockserver.model.HttpStatusCode
import org.mockserver.model.MediaType
import org.mockserver.verify.VerificationTimes
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses.riskPredictorResponseV1
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses.riskPredictorResponseV2
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses.successfulRiskSummaryResponse

class AssessRisksNeedsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    lateinit var assessRisksNeedsApi: AssessRisksNeedsMockServer
  }

  override fun beforeAll(context: ExtensionContext?) {
    assessRisksNeedsApi = AssessRisksNeedsMockServer()
  }

  override fun beforeEach(context: ExtensionContext?) {
    assessRisksNeedsApi.reset()
  }

  override fun afterAll(context: ExtensionContext?) {
    assessRisksNeedsApi.stop()
  }
}

class AssessRisksNeedsMockServer : ClientAndServer(MOCKSERVER_PORT) {

  companion object {
    private const val MOCKSERVER_PORT = 8085
  }

  fun riskPredictorV1Response(crn: String) {
    val request = HttpRequest.request().withPath("/risks/predictors/all/crn/$crn")
      .withHeader("Authorization")
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(riskPredictorResponseV1()),
    )
  }

  fun riskPredictorV2Response(crn: String) {
    val request = HttpRequest.request().withPath("/risks/predictors/all/crn/$crn")
      .withHeader("Authorization")
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(riskPredictorResponseV2()),
    )
  }

  fun riskPredictorErrorResponse(crn: String) {
    val request = HttpRequest.request().withPath("/risks/predictors/all/crn/$crn")
      .withHeader("Authorization")
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500.code()).withContentType(
        MediaType.APPLICATION_JSON,
      ).withBody("{}"),
    )
  }

  fun verifyRiskPredictorCalled(crn: String, times: Int) {
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.verify(
      HttpRequest.request()
        .withPath("/risks/predictors/all/crn/$crn")
        .withHeader("Authorization"),
      VerificationTimes.exactly(times),
    )
  }

  fun riskSummaryResponse(crn: String) {
    val request = HttpRequest.request().withPath("/risks/crn/$crn/summary")
      .withHeader("Authorization")
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.`when`(request, Times.exactly(2)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(successfulRiskSummaryResponse()),
    )
  }

  fun riskSummaryErrorResponse(crn: String) {
    val request = HttpRequest.request().withPath("/risks/crn/$crn/summary")
      .withHeader("Authorization")
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.`when`(request, Times.exactly(2)).respond(
      HttpResponse.response().withStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500.code()).withContentType(
        MediaType.APPLICATION_JSON,
      ).withBody("{}"),
    )
  }

  fun verifyRiskSummaryCalled(crn: String, times: Int) {
    AssessRisksNeedsApiExtension.assessRisksNeedsApi.verify(
      HttpRequest.request()
        .withPath("/risks/crn/$crn/summary")
        .withHeader("Authorization"),
      VerificationTimes.exactly(times),
    )
  }
}
