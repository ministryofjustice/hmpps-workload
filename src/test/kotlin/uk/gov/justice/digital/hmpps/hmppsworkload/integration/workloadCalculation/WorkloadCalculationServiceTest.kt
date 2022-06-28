package uk.gov.justice.digital.hmpps.hmppsworkload.integration.workloadCalculation

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WMTAssessmentEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WMTCMSEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WMTCourtReportsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WMTInstitutionalReportEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.service.WorkloadCalculationService
import java.math.BigInteger

internal class WorkloadCalculationServiceTest : IntegrationTestBase() {

  @Autowired
  protected lateinit var workloadCalculation: WorkloadCalculationService

  @Test
  fun `calculate WorkloadCalculation empty workload`() {

    val staffCode = "STAFF1"
    val teamCode = "TM1"
    val providerCode = "SC1"
    val staffGrade = "PO"

    workloadCalculation.calculate(staffCode, teamCode, providerCode, staffGrade)

    assertEquals(
      BigInteger.ZERO,
      workloadCalculationRepository
        .findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)?.workloadPoints
    )
  }

  @Test
  fun `breakdown data should include court report count`() {
    val staffCode = "STAFF1"
    val teamCode = "TM1"
    val providerCode = "SC1"
    val staffGrade = "PO"
    val standardDeliveryReportCount = 10
    val fastDeliveryReportCount = 0

    wmtCourtReportsRepository.save(
      WMTCourtReportsEntity(
        staffCode = staffCode, teamCode = teamCode,
        fastDeliveryReportCount = fastDeliveryReportCount, standardDeliveryReportCount = standardDeliveryReportCount
      )
    )
    workloadCalculation.calculate(staffCode, teamCode, providerCode, staffGrade)

    val workloadCalculationResult = workloadCalculationRepository
      .findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)

    Assertions.assertAll(
      {
        assertEquals(
          standardDeliveryReportCount,
          workloadCalculationResult?.breakdownData?.standardDeliveryReportCount
        )
      },
      {
        assertEquals(
          fastDeliveryReportCount,
          workloadCalculationResult?.breakdownData?.fastDeliveryReportCount
        )
      }
    )
  }

  @Test
  fun `breakdown data should include parole report count`() {
    val staffCode = "STAFF1"
    val teamCode = "TM1"
    val providerCode = "SC1"
    val staffGrade = "PO"
    val paroleReportsCount = 4

    wmtInstitutionalReportRepository.save(WMTInstitutionalReportEntity(staffCode = staffCode, teamCode = teamCode, paroleReports = paroleReportsCount))

    workloadCalculation.calculate(staffCode, teamCode, providerCode, staffGrade)

    val workloadCalculationResult = workloadCalculationRepository
      .findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)

    assertEquals(
      paroleReportsCount,
      workloadCalculationResult?.breakdownData?.paroleReportsCount
    )
  }

  @Test
  fun `breakdown data should include case assessment count`() {
    val staffCode = "STAFF1"
    val teamCode = "TM1"
    val providerCode = "SC1"
    val staffGrade = "PO"
    val communityAssessmentCount = 1
    val licenseAssessmentCount = 1

    wmtAssessmentRepository.save(WMTAssessmentEntity(staffCode = staffCode, teamCode = teamCode, sentenceType = "Community"))
    wmtAssessmentRepository.save(WMTAssessmentEntity(staffCode = staffCode, teamCode = teamCode, sentenceType = "License"))

    workloadCalculation.calculate(staffCode, teamCode, providerCode, staffGrade)

    val workloadCalculationResult = workloadCalculationRepository
      .findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)

    assertEquals(
      communityAssessmentCount,
      workloadCalculationResult?.breakdownData?.communityCaseAssessmentCount
    )

    assertEquals(
      licenseAssessmentCount,
      workloadCalculationResult?.breakdownData?.licenseCaseAssessmentCount
    )
  }

  @Test
  fun `breakdown data should include contacts performed outside and by others`() {
    val staffCode = "STAFF1"
    val teamCode = "TM1"
    val providerCode = "SC1"
    val staffGrade = "PO"
    val contactTypeCode = "CONTACT1"

    // increased
    wmtcmsRepository.save(
      WMTCMSEntity(
        staffCode = staffCode, staffTeamCode = teamCode, personManagerStaffCode = "OTHERSTAFFCODE",
        personManagerTeamCode = "TM2", contactTypeCode = contactTypeCode
      )
    )

    // decreased
    wmtcmsRepository.save(
      WMTCMSEntity(
        staffCode = "StaffCode", staffTeamCode = "TM2", personManagerStaffCode = staffCode,
        personManagerTeamCode = teamCode, contactTypeCode = contactTypeCode
      )
    )

    workloadCalculation.calculate(staffCode, teamCode, providerCode, staffGrade)

    val workloadCalculationResult = workloadCalculationRepository
      .findFirstByStaffCodeAndTeamCodeOrderByCalculatedDate(staffCode, teamCode)

    assertEquals(
      1,
      workloadCalculationResult?.breakdownData?.contactsPerformedByOthersCount?.get(contactTypeCode)
    )

    assertEquals(
      1,
      workloadCalculationResult?.breakdownData?.contactsPerformedOutsideCaseloadCount?.get(contactTypeCode)
    )
  }
}