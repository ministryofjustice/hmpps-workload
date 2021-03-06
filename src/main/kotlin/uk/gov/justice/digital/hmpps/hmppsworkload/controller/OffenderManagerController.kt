package uk.gov.justice.digital.hmpps.hmppsworkload.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseAllocated
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.ImpactCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerCases
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerOverview
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.OffenderManagerPotentialWorkload
import uk.gov.justice.digital.hmpps.hmppsworkload.service.SaveWorkloadService
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.GetOffenderManagerService
import javax.persistence.EntityNotFoundException

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class OffenderManagerController(
  private val getOffenderManagerService: GetOffenderManagerService,
  private val saveWorkloadService: SaveWorkloadService
) {

  @Operation(summary = "Retrieves capacity and potential capacity if case were to be allocated")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_WORKLOAD_MEASUREMENT') or hasRole('ROLE_WORKLOAD_READ')")
  @PostMapping("/team/{teamCode}/offenderManager/{staffCode}/impact")
  fun getImpactOfAllocation(@PathVariable(required = true) teamCode: String, @PathVariable(required = true) staffCode: String, @RequestBody impactCase: ImpactCase): OffenderManagerPotentialWorkload {
    val potentialWorkload = getOffenderManagerService.getPotentialWorkload(teamCode, staffCode, impactCase)
    if (potentialWorkload != null) {
      return OffenderManagerPotentialWorkload.from(potentialWorkload)
    }
    throw EntityNotFoundException("Team $teamCode and staff Code $staffCode combination not found")
  }

  @Operation(summary = "Retrieves overview of Offender Manager")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_WORKLOAD_MEASUREMENT') or hasRole('ROLE_WORKLOAD_READ')")
  @GetMapping("/team/{teamCode}/offenderManagers/{offenderManagerCode}")
  fun getOverview(@PathVariable(required = true) teamCode: String, @PathVariable(required = true) offenderManagerCode: String): OffenderManagerOverview {
    return getOffenderManagerService.getOverview(teamCode, offenderManagerCode)?.let {
      OffenderManagerOverview.from(it)
    } ?: run {
      throw EntityNotFoundException("Team $teamCode and offender manager $offenderManagerCode combination not found")
    }
  }

  @Operation(summary = "Allocate Case to an Offender Manager")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @PostMapping("/team/{teamCode}/offenderManager/{staffCode}/case")
  fun allocateCaseToOffenderManager(
    @PathVariable(required = true) teamCode: String,
    @PathVariable(required = true) staffCode: String,
    @RequestBody allocateCase: AllocateCase,
    authentication: Authentication,
    @RequestHeader(
      HttpHeaders.AUTHORIZATION
    ) authToken: String
  ): CaseAllocated {
    return saveWorkloadService.saveWorkload(teamCode, staffCode, allocateCase, authentication.name, authToken)
  }

  @Operation(summary = "Retrieves all cases allocated to an Offender Manager")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_WORKLOAD_MEASUREMENT') or hasRole('ROLE_WORKLOAD_READ')")
  @GetMapping("/team/{teamCode}/offenderManagers/{offenderManagerCode}/cases")
  fun getCases(@PathVariable(required = true) teamCode: String, @PathVariable(required = true) offenderManagerCode: String): OffenderManagerCases {
    return getOffenderManagerService.getCases(teamCode, offenderManagerCode)
      ?: run {
        throw EntityNotFoundException("Team $teamCode and offender manager $offenderManagerCode combination not found")
      }
  }
}
