package uk.gov.justice.digital.hmpps.hmppsworkload.integration.workloadCalculation

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.AdjustmentReasonEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PersonManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WMTCMSEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WMTCourtReportsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.service.WorkloadCalculationService
import java.math.BigInteger

internal class WorkloadCalculationServiceTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var workloadCalculation: WorkloadCalculationService

  @Test
  fun `calculate WorkloadCalculation empty workload`() {
    val staffCode = "STAFF1"
    val teamCode = "TM1"
    val staffGrade = "PO"

    workloadCalculation.saveWorkloadCalculation(StaffIdentifier(staffCode, teamCode), staffGrade)

    await untilCallTo {
      workloadCalculationRepository.count()
    } matches { it == 1L }

    assertEquals(BigInteger.ZERO, workloadCalculationRepository.findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)?.workloadPoints)
  }

  @Test
  fun `breakdown data should include court report count`() {
    val staffCode = "STAFF1"
    val teamCode = "TM1"
    val staffGrade = "PO"
    val standardDeliveryReportCount = 10
    val fastDeliveryReportCount = 0

    wmtCourtReportsRepository
      .save(WMTCourtReportsEntity(staffCode = staffCode, teamCode = teamCode, fastDeliveryReportCount = fastDeliveryReportCount, standardDeliveryReportCount = standardDeliveryReportCount))
    workloadCalculation.saveWorkloadCalculation(StaffIdentifier(staffCode, teamCode), staffGrade)

    await untilCallTo {
      workloadCalculationRepository.count()
    } matches { it == 1L }

    val workloadCalculationResult = workloadCalculationRepository
      .findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)

    Assertions.assertAll(
      { assertEquals(standardDeliveryReportCount, workloadCalculationResult?.breakdownData?.standardDeliveryReportCount) },
      { assertEquals(fastDeliveryReportCount, workloadCalculationResult?.breakdownData?.fastDeliveryReportCount) },
    )
  }

  @Test
  fun `breakdown data should include contacts performed outside and by others`() {
    val staffCode = "STAFF1"
    val teamCode = "TM1"
    val staffGrade = "PO"
    val contactTypeCode = "CONTACT1"

    wmtcmsRepository
      .save(WMTCMSEntity(staffCode = staffCode, staffTeamCode = teamCode, personManagerStaffCode = "OTHERSTAFFCODE", personManagerTeamCode = "TM2", contactTypeCode = contactTypeCode))

    wmtcmsRepository
      .save(WMTCMSEntity(staffCode = "StaffCode", staffTeamCode = "TM2", personManagerStaffCode = staffCode, personManagerTeamCode = teamCode, contactTypeCode = contactTypeCode))

    workloadCalculation.saveWorkloadCalculation(StaffIdentifier(staffCode, teamCode), staffGrade)

    await untilCallTo {
      workloadCalculationRepository.count()
    } matches { it == 1L }
    val workloadCalculationResult = workloadCalculationRepository
      .findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)

    Assertions.assertAll(
      { assertEquals(1, workloadCalculationResult?.breakdownData?.contactsPerformedByOthersCount?.get(contactTypeCode)) },
      { assertEquals(1, workloadCalculationResult?.breakdownData?.contactsPerformedOutsideCaseloadCount?.get(contactTypeCode)) },
    )
  }

  @Test
  fun `must include contact type weightings in breakdown`() {
    val staffCode = "STAFF1"
    val teamCode = "TM1"
    val staffGrade = "PO"

    val adjustmentReason = AdjustmentReasonEntity(typeCode = "ADJUSTMENT_REASON1", points = 10)
    adjustmentReasonRepository.save(adjustmentReason)

    workloadCalculation.saveWorkloadCalculation(StaffIdentifier(staffCode, teamCode), staffGrade)

    await untilCallTo {
      workloadCalculationRepository.count()
    } matches { it == 1L }

    val breakdown = workloadCalculationRepository
      .findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)!!.breakdownData

    assertEquals(adjustmentReason.points, breakdown.contactTypeWeightings[adjustmentReason.typeCode])
  }

  @Test
  fun `must include caseload count in breakdown`() {
    val staffCode = "STAFF1"
    val teamCode = "TM1"
    val staffGrade = "PO"

    personManagerRepository.save(PersonManagerEntity(crn = "CRN1", staffCode = staffCode, teamCode = teamCode, createdBy = "USER", isActive = true, allocationReason = AllocationReason.INITIAL_ALLOCATION))

    caseDetailsRepository.save(CaseDetailsEntity("CRN1", Tier.B2, CaseType.COMMUNITY, "Jane", "Doe"))

    workloadCalculation.saveWorkloadCalculation(StaffIdentifier(staffCode, teamCode), staffGrade)

    await untilCallTo {
      workloadCalculationRepository.count()
    } matches { it == 1L }

    val breakdown = workloadCalculationRepository
      .findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)!!.breakdownData

    assertEquals(1, breakdown.caseloadCount)
  }
}
