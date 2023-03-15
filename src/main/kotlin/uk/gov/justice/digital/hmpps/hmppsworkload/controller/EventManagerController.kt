package uk.gov.justice.digital.hmpps.hmppsworkload.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.CompleteDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CreatedAllocationDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.EventManagerDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.JpaBasedGetEventManager
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.EntityNotFoundException

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class EventManagerController(private val getEventManager: JpaBasedGetEventManager) {

  @Operation(summary = "Get Event Manager by ID")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_WORKLOAD_MEASUREMENT') or hasRole('ROLE_WORKLOAD_READ')")
  @GetMapping("\${event.manager.getByIdPath}")
  suspend fun getEventManagerById(@PathVariable(required = true) id: UUID): EventManagerDetails =
    getEventManager.findById(id)?.let { eventManagerEntity -> EventManagerDetails.from(eventManagerEntity) }
      ?: throw EntityNotFoundException("Event Manager not found for id $id")

  @Operation(summary = "Get case details of Event Manager by crn and event number")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_WORKLOAD_MEASUREMENT') or hasRole('ROLE_WORKLOAD_READ')")
  @GetMapping("/allocation/person/{crn}/event/{eventNumber}/details")
  suspend fun getCaseDetailsForEventManager(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) eventNumber: Int
  ): CaseDetails =
    getEventManager.findDetailsByCrnAndEventNumber(crn, eventNumber) ?: throw EntityNotFoundException("Case details of event manager not found for crn $crn eventNumber $eventNumber")

  @Operation(summary = "Get Allocation complete details of Event Manager by crn and event number")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_WORKLOAD_MEASUREMENT') or hasRole('ROLE_WORKLOAD_READ')")
  @GetMapping("/allocation/person/{crn}/event/{eventNumber}/complete-details")
  suspend fun getCompleteDetailsForEventManager(
    @PathVariable(required = true) crn: String,
    @PathVariable(required = true) eventNumber: Int
  ): CompleteDetails =
    getEventManager.findCompleteDetailsByCrnAndEventNumber(crn, eventNumber) ?: throw EntityNotFoundException("Complete details of event manager not found for crn $crn eventNumber $eventNumber")

  @Operation(summary = "Get allocated events created by logged in user")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK")
    ]
  )
  @PreAuthorize("hasRole('ROLE_WORKLOAD_MEASUREMENT') or hasRole('ROLE_WORKLOAD_READ')")
  @GetMapping("/allocation/events/me")
  suspend fun getAllocationsByLoggedInUser(@RequestParam(required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) since: ZonedDateTime, authentication: Authentication): CreatedAllocationDetails =
    getEventManager.findAllocationsBy(since, authentication.name)
}
