package uk.gov.justice.digital.hmpps.hmppsworkload.integration.caseload

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Case
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.service.GetCombinedCaseload

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
    caseDetailsRepository.save(CaseDetailsEntity(realtimeCase.crn, realtimeCase.tier, realtimeCase.type, "Jane", "Doe"))
    val wmtStaff = setupCurrentWmtStaff(staffCode, teamCode)
    setupWmtManagedCase(wmtStaff, realtimeCase.tier, realtimeCase.crn, realtimeCase.type)

    val actualCases = getCaseLoad.getCases(StaffIdentifier("OM1", "T1"))

    Assertions.assertEquals(realtimeCase, actualCases[0])
  }

  @Test
  fun `must not return list of cases if no realtime data exist`() {
    val staffCode = "OM1"
    val teamCode = "T1"
    val wmtStaff = setupCurrentWmtStaff(staffCode, teamCode)
    setupWmtManagedCase(wmtStaff, Tier.C2, "CRN12345", CaseType.COMMUNITY)

    Assertions.assertEquals(0, getCaseLoad.getCases(StaffIdentifier("OM1", "T1")).size)
  }

  @Test
  fun `must return list of person manager cases when realtime data exists`() {
    val staffCode = "OM1"
    val teamCode = "T1"

    val realtimeCase = Case(Tier.A1, CaseType.LICENSE, false, "CRN1112")

    personManagerRepository.save(
      PersonManagerEntity(
        crn = realtimeCase.crn,
        staffCode = staffCode,
        teamCode = teamCode,
        createdBy = "createdBy",
        isActive = true,
        allocationReason = AllocationReason.INITIAL_ALLOCATION,
      ),
    )

    // realtime
    caseDetailsRepository.save(CaseDetailsEntity(realtimeCase.crn, realtimeCase.tier, CaseType.LICENSE, "Jane", "Doe"))

    val actualCases = getCaseLoad.getCases(StaffIdentifier("OM1", "T1"))

    Assertions.assertEquals(realtimeCase.crn, actualCases[0].crn)
  }

  @Test
  fun `must return one case when offender manager and person manager have entries for the same CRN`() {
    val staffCode = "OM1"
    val teamCode = "T1"

    val realtimeCase = Case(Tier.A1, CaseType.LICENSE, false, "CRN1111")
    caseDetailsRepository.save(CaseDetailsEntity(realtimeCase.crn, realtimeCase.tier, realtimeCase.type, "Jane", "Doe"))

    personManagerRepository.save(
      PersonManagerEntity(
        crn = realtimeCase.crn,
        staffCode = staffCode,
        teamCode = teamCode,
        createdBy = "createdBy",
        isActive = true,
        allocationReason = AllocationReason.INITIAL_ALLOCATION,
      ),
    )

    val wmtStaff = setupCurrentWmtStaff(staffCode, teamCode)
    setupWmtManagedCase(wmtStaff, realtimeCase.tier, realtimeCase.crn, realtimeCase.type)

    val actualCases = getCaseLoad.getCases(StaffIdentifier("OM1", "T1"))

    Assertions.assertEquals(1, actualCases.size)
  }

  @Test
  fun `must not be in caseload if allocated to another staff member`() {
    val originalStaffCode = "STAFF1"
    val teamCode = "TEAM!"
    val newStaffCode = "STAFF2"

    val realtimeCase = Case(Tier.A1, CaseType.LICENSE, false, "CRN9191")
    caseDetailsRepository.save(CaseDetailsEntity(realtimeCase.crn, realtimeCase.tier, CaseType.LICENSE, "Jane", "Doe"))

    personManagerRepository.save(
      PersonManagerEntity(
        crn = realtimeCase.crn,
        staffCode = originalStaffCode,
        teamCode = teamCode,
        createdBy = "createdBy",
        isActive = false,
        allocationReason = AllocationReason.INITIAL_ALLOCATION,
      ),
    )

    personManagerRepository.save(
      PersonManagerEntity(
        crn = realtimeCase.crn,
        staffCode = newStaffCode,
        teamCode = teamCode,
        createdBy = "createdBy",
        isActive = true,
        allocationReason = AllocationReason.INITIAL_ALLOCATION,
      ),
    )

    val actualCases = getCaseLoad.getCases(StaffIdentifier("STAFF1", "TEAM!"))

    Assertions.assertEquals(0, actualCases.size)
  }
}
