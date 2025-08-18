package uk.gov.justice.digital.hmpps.hmppsworkload.client

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchangeOrNull
import org.springframework.web.reactive.function.client.createExceptionAndAwait
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocationDemandDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocationDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocationDetailsRequest
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ChoosePractitionerResponse
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.CommunityPersonManager
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.CompleteDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.DeliusTeams
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ImpactResponse
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Name
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.OfficerView
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.PersonSummary
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ProbationStatus
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffActiveCases
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity

const val DOWNSTREAM_500 = "Downstream 5xx:"

@Suppress("SwallowedException", "TooManyFunctions", "LargeClass", "StringLiteralDuplication")
class WorkforceAllocationsToDeliusApiClient(private val webClient: WebClient) {

  companion object {
    val log = LoggerFactory.getLogger(this::class.java)
  }

  suspend fun choosePractitioners(crn: String, teamCodes: List<String>): ChoosePractitionerResponse? {
    val teams = teamCodes.joinToString(separator = ",")
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/allocation-demand/choose-practitioner?crn={crn}&teamCode={teams}", crn, teams)
          .awaitExchangeOrNull { response ->
            when {
              response.statusCode() == HttpStatus.OK -> response.awaitBody()
              response.statusCode() == HttpStatus.NOT_FOUND -> null
              response.statusCode().is5xxServerError -> throw WorkloadFailedDependencyException("$DOWNSTREAM_500 ${response.statusCode()}")
              else -> throw response.createExceptionAndAwait()
            }
          }
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    } catch (e: WorkloadFailedDependencyException) {
      log.warn("DeliusClient choose practitioners failed due to Failed Dependency", e)
      throw WorkloadFailedDependencyException(e.message!!)
    }
  }

  suspend fun choosePractitioners(teamCodes: List<String>): ChoosePractitionerResponse? {
    val teams = teamCodes.joinToString(separator = ",")
    val responseString: String = getTeams(teams)
    if (responseString.isNullOrBlank()) return null
    val objectMapper = jacksonObjectMapper()
    val teamDetails: Map<String, Map<String, List<StaffMember>>> =
      objectMapper.readValue(
        responseString,
        object : TypeReference<Map<String, Map<String, List<StaffMember>>>>() {},
      )
    val teamDetail = teamDetails["teams"]?.values?.flatten() ?: emptyList()
    return createPractitionersResponse(
      teamDetails.keys.first(),
      teamDetail.map { StaffMember(it.code, it.name, it.email, it.retrieveGrade()) },
    )
  }

  suspend fun getTeams(teams: String): String {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/teams?teamCode={teams}", teams)
          .awaitExchangeOrNull { response ->
            when {
              response.statusCode() == HttpStatus.OK -> response.awaitBody()
              response.statusCode() == HttpStatus.NOT_FOUND -> null
              response.statusCode().is5xxServerError -> throw WorkloadFailedDependencyException("$DOWNSTREAM_500 ${response.statusCode()}")
              else -> throw response.createExceptionAndAwait()
            }
          } ?: ""
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    } catch (e: WorkloadFailedDependencyException) {
      log.warn("DeliusClient get teams failed due to Failed Dependency", e)
      throw WorkloadFailedDependencyException(e.message!!)
    }
  }

  private fun createPractitionersResponse(teams: String, staffMembers: List<StaffMember>): ChoosePractitionerResponse? {
    val nullName = Name("", "", "")
    val nullProbationStatus = ProbationStatus("", "")
    val nullCommunityPersonManager = CommunityPersonManager("", nullName, "", "")
    return ChoosePractitionerResponse(
      "",
      nullName,
      nullProbationStatus,
      nullCommunityPersonManager,
      mapOf(teams to staffMembers),
    )
  }

  suspend fun getPersonByCrn(crn: String): PersonSummary? = getPerson(crn, "CRN") { response ->
    when {
      response.statusCode() == HttpStatus.OK -> response.awaitBody()
      response.statusCode() == HttpStatus.NOT_FOUND ->
        PersonSummary(crn, Name("Unknown", "", "Unknown"), CaseType.UNKNOWN)
      response.statusCode().is5xxServerError ->
        throw WorkloadFailedDependencyException("DeliusClient getPersonByCrn failed with ${response.statusCode()}")
      else -> throw response.createExceptionAndAwait()
    }
  }

  suspend fun getPersonByNoms(noms: String): PersonSummary? = getPerson(noms, "NOMS") { response ->
    when {
      response.statusCode() == HttpStatus.OK -> response.awaitBody()
      response.statusCode() == HttpStatus.NOT_FOUND -> null
      response.statusCode().is5xxServerError ->
        throw WorkloadFailedDependencyException("DeliusClient getPersonByNoms failed with ${response.statusCode()}")
      else -> throw response.createExceptionAndAwait()
    }
  }

  private suspend fun getPerson(
    identifier: String,
    identifierType: String,
    responseHandler: suspend (ClientResponse) -> PersonSummary?,
  ): PersonSummary? {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/person/{identifier}?type={identifierType}", identifier, identifierType)
          .awaitExchangeOrNull(responseHandler)
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getOfficerView(staffCode: String): OfficerView {
    try {
      return withTimeout(OFFICER_VIEW_TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/staff/{staffCode}/officer-view", staffCode)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { response ->
            response.createException().flatMap { Mono.error(WorkloadFailedDependencyException(it.message!!)) }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun impact(crn: String, staffCode: String): ImpactResponse {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/allocation-demand/impact?crn={crn}&staff={staffCode}", crn, staffCode)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { response ->
            response.createException().flatMap { Mono.error(WorkloadFailedDependencyException(it.message!!)) }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun allocationCompleteDetails(crn: String, eventNumber: String, staffCode: String): CompleteDetails {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri(
            "/allocation-completed/details?crn={crn}&eventNumber={eventNumber}&staffCode={staffCode}",
            crn,
            eventNumber,
            staffCode,
          )
          .retrieve()
          .onStatus({ it.is5xxServerError }) { response ->
            response.createException().flatMap { Mono.error(WorkloadFailedDependencyException(it.message!!)) }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun staffActiveCases(staffCode: String, crns: Collection<String>): StaffActiveCases {
    val requestType = object : ParameterizedTypeReference<Collection<String>>() {}
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .post()
          .uri("/staff/{staffCode}/active-cases", staffCode)
          .body(Mono.just(crns), requestType)
          .retrieve()
          .onStatus({ it.is5xxServerError }) { response ->
            response.createException().flatMap { Mono.error(WorkloadFailedDependencyException(it.message!!)) }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun allocationDetails(
    crn: String,
    eventNumber: Int,
    staffCode: String,
    loggedInUser: String,
  ): AllocationDemandDetails {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri(
            "/allocation-demand/{crn}/{eventNumber}/allocation?staff={staffCode}&allocatingStaffUsername={loggedInUser}",
            crn,
            eventNumber,
            staffCode,
            loggedInUser,
          )
          .retrieve()
          .onStatus({ it.is5xxServerError }) { response ->
            response.createException().flatMap { Mono.error(WorkloadFailedDependencyException(it.message!!)) }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun allocationDetails(eventManagers: List<EventManagerEntity>): AllocationDetails {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .post()
          .uri("/allocation/details")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(AllocationDetailsRequest.from(eventManagers))
          .retrieve()
          .onStatus({ it.is5xxServerError }) { response ->
            response.createException().flatMap { Mono.error(WorkloadFailedDependencyException(it.message!!)) }
          }
          .awaitBody()
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    }
  }

  suspend fun getDeliusAllowedTeamInfo(staffId: String): DeliusTeams {
    val responseString = getDeliusTeamsResponse(staffId)
    val objectMapper = ObjectMapper()
    val teamsResponse = objectMapper.readValue(responseString, DeliusTeams::class.java)
    return teamsResponse ?: DeliusTeams(emptyList(), emptyList())
  }

  private suspend fun getDeliusTeamsResponse(staffId: String): String {
    try {
      return withTimeout(TIMEOUT_VALUE) {
        webClient
          .get()
          .uri("/staff/$staffId/teams", staffId)
          .awaitExchangeOrNull { response ->
            when {
              response.statusCode() == HttpStatus.OK -> response.awaitBody()
              response.statusCode() == HttpStatus.NOT_FOUND -> null
              response.statusCode().is5xxServerError -> throw WorkloadFailedDependencyException("$DOWNSTREAM_500 ${response.statusCode()}")
              else -> throw response.createExceptionAndAwait()
            }
          } ?: ""
      }
    } catch (e: TimeoutCancellationException) {
      throw WorkloadWebClientTimeoutException(e.message!!)
    } catch (e: WorkloadFailedDependencyException) {
      log.warn("DeliusClient getDeliusTeamsResponse failed due to Failed Dependency", e)
      throw WorkloadFailedDependencyException(e.message!!)
    }
  }
}
