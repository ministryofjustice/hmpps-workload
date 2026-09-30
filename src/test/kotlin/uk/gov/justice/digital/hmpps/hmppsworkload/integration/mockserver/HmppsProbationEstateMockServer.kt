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
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.TeamOverview
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.HmppsProbationEstateApiExtension.Companion.hmppsProbationEstate

class HmppsProbationEstateApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    lateinit var hmppsProbationEstate: HmppsProbationEstateMockServer
  }

  override fun beforeAll(context: ExtensionContext?) {
    hmppsProbationEstate = HmppsProbationEstateMockServer()
  }

  override fun beforeEach(context: ExtensionContext?) {
    hmppsProbationEstate.reset()
  }

  override fun afterAll(context: ExtensionContext?) {
    hmppsProbationEstate.stop()
  }
}

class HmppsProbationEstateMockServer : ClientAndServer(MOCKSERVER_PORT) {
  companion object {
    private const val MOCKSERVER_PORT = 8086
  }

  fun getTeamsResponse(teams: List<TeamOverview>) {
    val request = HttpRequest.request().withPath("/team/search").withQueryStringParameter("codes", *teams.map { it.code }.toTypedArray())
    hmppsProbationEstate.`when`(request, Times.exactly(1)).respond(
      HttpResponse.response().withContentType(MediaType.APPLICATION_JSON).withBody(JsonBody.json(teams)),
    )
  }
}
