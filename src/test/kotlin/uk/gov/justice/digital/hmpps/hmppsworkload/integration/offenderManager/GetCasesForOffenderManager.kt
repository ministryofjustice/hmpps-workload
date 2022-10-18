package uk.gov.justice.digital.hmpps.hmppsworkload.integration.offenderManager

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity

class GetCasesForOffenderManager : IntegrationTestBase() {

  @Test
  fun `get all cases allocated to offender manager`() {
    val staffCode = "OM1"
    val teamCode = "T1"
    staffCodeResponse(staffCode, teamCode)
    val realTimeCaseDetails = caseDetailsRepository.saveAll(listOf(CaseDetailsEntity("CRN2222", Tier.B3, CaseType.CUSTODY, "Sally", "Smith"), CaseDetailsEntity("CRN3333", Tier.C1, CaseType.COMMUNITY, "John", "Williams"), CaseDetailsEntity("CRN1111", Tier.C1, CaseType.LICENSE, "John", "Doe")))
    val wmtStaff = setupCurrentWmtStaff(staffCode, teamCode)

    realTimeCaseDetails.forEach { caseDetails ->
      setupWmtManagedCase(wmtStaff, caseDetails.tier, caseDetails.crn, caseDetails.type)
    }

    webTestClient.get()
      .uri("/team/$teamCode/offenderManagers/$staffCode/cases")
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.forename")
      .isEqualTo("Sheila")
      .jsonPath("$.surname")
      .isEqualTo("Hancock")
      .jsonPath("$.grade")
      .isEqualTo("PO")
      .jsonPath("$.code")
      .isEqualTo(staffCode)
      .jsonPath("$.teamName")
      .isEqualTo("Test Team")
      .jsonPath("$.activeCases[?(@.crn == 'CRN2222')].tier")
      .isEqualTo("B3")
      .jsonPath("$.activeCases[?(@.crn == 'CRN2222')].caseCategory")
      .isEqualTo("CUSTODY")
      .jsonPath("$.activeCases[?(@.crn == 'CRN2222')].forename")
      .isEqualTo("Sally")
      .jsonPath("$.activeCases[?(@.crn == 'CRN2222')].surname")
      .isEqualTo("Smith")
      .jsonPath("$.activeCases[?(@.crn == 'CRN3333')].tier")
      .isEqualTo("C1")
      .jsonPath("$.activeCases[?(@.crn == 'CRN3333')].caseCategory")
      .isEqualTo("COMMUNITY")
      .jsonPath("$.activeCases[?(@.crn == 'CRN3333')].forename")
      .isEqualTo("John")
      .jsonPath("$.activeCases[?(@.crn == 'CRN3333')].surname")
      .isEqualTo("Williams")
      .jsonPath("$.activeCases[?(@.crn == 'CRN1111')].tier")
      .isEqualTo("C1")
      .jsonPath("$.activeCases[?(@.crn == 'CRN1111')].caseCategory")
      .isEqualTo("LICENSE")
      .jsonPath("$.activeCases[?(@.crn == 'CRN1111')].forename")
      .isEqualTo("John")
      .jsonPath("$.activeCases[?(@.crn == 'CRN1111')].surname")
      .isEqualTo("Doe")
  }

  @Test
  fun `Get staff member without any WMT active cases`() {
    val staffCode = "NOWORKLOAD1"
    val teamCode = "T1"
    staffCodeResponse(staffCode, teamCode)
    webTestClient.get()
      .uri("/team/$teamCode/offenderManagers/$staffCode/cases")
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.forename")
      .isEqualTo("Sheila")
      .jsonPath("$.surname")
      .isEqualTo("Hancock")
      .jsonPath("$.grade")
      .isEqualTo("PO")
      .jsonPath("$.code")
      .isEqualTo(staffCode)
      .jsonPath("$.teamName")
      .isEqualTo("Test Team")
      .jsonPath("$.activeCases")
      .isEmpty
  }

  @Test
  fun `still return response if not all offender details are returned`() {
    val staffCode = "OM1"
    val teamCode = "T1"
    staffCodeResponse(staffCode, teamCode)
    val caseDetails = caseDetailsRepository.save(CaseDetailsEntity("CRN1111", Tier.C1, CaseType.LICENSE, "John", "Doe"))
    val wmtStaff = setupCurrentWmtStaff(staffCode, teamCode)
    setupWmtManagedCase(wmtStaff, caseDetails.tier, caseDetails.crn, caseDetails.type)
    setupWmtManagedCase(wmtStaff, Tier.B3, "CRN2222", CaseType.CUSTODY)
    setupWmtManagedCase(wmtStaff, Tier.C1, "CRN3333", CaseType.COMMUNITY)

    webTestClient.get()
      .uri("/team/$teamCode/offenderManagers/$staffCode/cases")
      .headers {
        it.authToken(roles = listOf("ROLE_WORKLOAD_MEASUREMENT"))
      }
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("$.forename")
      .isEqualTo("Sheila")
      .jsonPath("$.surname")
      .isEqualTo("Hancock")
      .jsonPath("$.grade")
      .isEqualTo("PO")
      .jsonPath("$.code")
      .isEqualTo(staffCode)
      .jsonPath("$.teamName")
      .isEqualTo("Test Team")
      .jsonPath("$.activeCases[?(@.crn == 'CRN2222')].tier")
      .isEqualTo("B3")
      .jsonPath("$.activeCases[?(@.crn == 'CRN2222')].caseCategory")
      .isEqualTo("CUSTODY")
      .jsonPath("$.activeCases[?(@.crn == 'CRN3333')].tier")
      .isEqualTo("C1")
      .jsonPath("$.activeCases[?(@.crn == 'CRN3333')].caseCategory")
      .isEqualTo("COMMUNITY")
      .jsonPath("$.activeCases[?(@.crn == 'CRN1111')].tier")
      .isEqualTo("C1")
      .jsonPath("$.activeCases[?(@.crn == 'CRN1111')].caseCategory")
      .isEqualTo("LICENSE")
      .jsonPath("$.activeCases[?(@.crn == 'CRN1111')].forename")
      .isEqualTo("John")
      .jsonPath("$.activeCases[?(@.crn == 'CRN1111')].surname")
      .isEqualTo("Doe")
  }
}
