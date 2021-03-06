package uk.gov.justice.digital.hmpps.hmppsworkload.integration.caseload

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Case
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.service.GetCombinedCaseload
import java.math.BigInteger

class GetCombinedCaseloadTest : IntegrationTestBase() {

  private lateinit var getCaseLoad: GetCombinedCaseload

  @BeforeAll
  fun setup() {
    getCaseLoad = GetCombinedCaseload(offenderManagerRepository, personManagerRepository, caseDetailsRepository)
  }

  @Test
  fun `must return list of wmt cases`() {
    val staffCode = "OM1"
    val teamCode = "T1"

    val realtimeCase = Case(Tier.A1, CaseType.LICENSE, false, "CRN1111")
    caseDetailsRepository.save(CaseDetailsEntity(realtimeCase.crn, realtimeCase.tier, CaseType.LICENSE))

    val actualCases = getCaseLoad.getCases(staffCode, teamCode)

    Assertions.assertEquals(realtimeCase, actualCases[0])
  }

  @Test
  fun `must not return list of cases if no realtime data exist`() {
    val staffCode = "OM1"
    val teamCode = "T1"

    Assertions.assertEquals(0, getCaseLoad.getCases(staffCode, teamCode).size)
  }

  @Test
  fun `must return list of person manager cases when realtime data exists`() {
    val staffCode = "OM1"
    val teamCode = "T1"

    val realtimeCase = Case(Tier.A1, CaseType.LICENSE, false, "CRN1112")

    personManagerRepository.save(
      PersonManagerEntity(
        crn = realtimeCase.crn, staffCode = staffCode,
        teamCode = teamCode, staffId = BigInteger.TEN, offenderName = "offenderName", createdBy = "createdBy",
        providerCode = "providerCode"
      )
    )

    // realtime
    caseDetailsRepository.save(CaseDetailsEntity(realtimeCase.crn, realtimeCase.tier, CaseType.LICENSE))

    val actualCases = getCaseLoad.getCases(staffCode, teamCode)

    Assertions.assertEquals(realtimeCase.crn, actualCases[0].crn)
  }

  @Test
  fun `must return one case when offender manager and person manager have entries for the same CRN`() {

    val staffCode = "OM1"
    val teamCode = "T1"

    val realtimeCase = Case(Tier.A1, CaseType.LICENSE, false, "CRN1111")
    caseDetailsRepository.save(CaseDetailsEntity(realtimeCase.crn, realtimeCase.tier, CaseType.LICENSE))

    personManagerRepository.save(
      PersonManagerEntity(
        crn = realtimeCase.crn, staffCode = staffCode,
        teamCode = teamCode, staffId = BigInteger.TEN, offenderName = "offenderName", createdBy = "createdBy",
        providerCode = "providerCode"
      )
    )

    val actualCases = getCaseLoad.getCases(staffCode, teamCode)

    Assertions.assertEquals(1, actualCases.size)
  }
}
