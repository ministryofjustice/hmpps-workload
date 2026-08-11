package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Contact
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CourtReport
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CourtReportType
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WorkloadPointsEntity
import java.math.BigDecimal
import java.math.BigInteger
import java.time.ZonedDateTime

class WorkloadCalculatorTests {
  private val workloadCalculator = WorkloadCalculator()

  @Test
  fun `must sum all court reports`() {
    val standardCourtReportWeighting = BigInteger.TEN
    val fastCourtReportWeighting = BigInteger.TWO
    val t2aWorkloadPoints = mockWorkloadPoints(isT2A = true)
    val workloadPoints = mockWorkloadPoints(standardCourtReportWeighting = standardCourtReportWeighting, fastCourtReportWeighting = fastCourtReportWeighting, isT2A = false)
    val numberOfStandardCourtReports = 7
    val numberOfFastCourtReports = 12
    val standardCourtReports = (1..numberOfStandardCourtReports).map { CourtReport(CourtReportType.STANDARD) }
    val fastCourtReports = (1..numberOfFastCourtReports).map { CourtReport(CourtReportType.FAST) }

    val result = workloadCalculator.getWorkloadPoints(
      WorkloadPointsElements(
        emptyList(),
        standardCourtReports + fastCourtReports,
        emptyList(),
        emptyList(),
        emptyMap(),
        t2aWorkloadPoints,
        workloadPoints,
      ),
    )

    val expectedStandardCourtReportPoints = standardCourtReportWeighting.multiply(numberOfStandardCourtReports.toBigInteger())
    val expectedFastCourtReportPoints = fastCourtReportWeighting.multiply(numberOfFastCourtReports.toBigInteger())

    Assertions.assertEquals(expectedStandardCourtReportPoints.add(expectedFastCourtReportPoints), result)
  }

  @Test
  fun `must sum all contacts which occurred on cases outside of case management`() {
    val contactReasonWeightings = mapOf("CONTACT1" to 10, "CONTACT2" to 2)

    val numberOfContactOneContacts = 5
    val numberOfContactTwoContacts = 10

    val t2aWorkloadPoints = mockWorkloadPoints(isT2A = true)
    val workloadPoints = mockWorkloadPoints(isT2A = false)

    val contactOneContacts = (1..numberOfContactOneContacts).map { Contact("CONTACT1") }
    val contactTwoContacts = (1..numberOfContactTwoContacts).map { Contact("CONTACT2") }

    val result = workloadCalculator.getWorkloadPoints(
      WorkloadPointsElements(
        emptyList(),
        emptyList(),
        contactOneContacts + contactTwoContacts,
        emptyList(),
        contactReasonWeightings,
        t2aWorkloadPoints,
        workloadPoints,
      ),
    )

    val expectedContactOnePointsTotal = contactReasonWeightings["CONTACT1"]!! * numberOfContactOneContacts
    val expectedContactTwoPointsTotal = contactReasonWeightings["CONTACT2"]!! * numberOfContactTwoContacts

    Assertions.assertEquals((expectedContactOnePointsTotal + expectedContactTwoPointsTotal).toBigInteger(), result)
  }

  @Test
  fun `must not count contacts where the contact reason does not have a weighting`() {
    val contactReasonWeightings = mapOf("CONTACT1" to 10, "CONTACT2" to 2)

    val t2aWorkloadPoints = mockWorkloadPoints(isT2A = true)
    val workloadPoints = mockWorkloadPoints(isT2A = false)

    val unknownContacts = (1..5).map { Contact("ANUNKNOWNCONTACTTYPE") }

    val result = workloadCalculator.getWorkloadPoints(
      WorkloadPointsElements(
        emptyList(),
        emptyList(),
        unknownContacts,
        emptyList(),
        contactReasonWeightings,
        t2aWorkloadPoints,
        workloadPoints,
      ),
    )

    Assertions.assertEquals(BigInteger.ZERO, result)
  }

  @Test
  fun `contacts which occur within case management by others must be negative`() {
    val contactReasonWeightings = mapOf("CONTACT1" to 10, "CONTACT2" to 2)

    val t2aWorkloadPoints = mockWorkloadPoints(isT2A = true)
    val workloadPoints = mockWorkloadPoints(isT2A = false)

    val contacts = listOf(Contact("CONTACT1"), Contact("CONTACT2"))

    val result = workloadCalculator.getWorkloadPoints(
      WorkloadPointsElements(
        emptyList(),
        emptyList(),
        emptyList(),
        contacts,
        contactReasonWeightings,
        t2aWorkloadPoints,
        workloadPoints,
      ),
    )

    Assertions.assertEquals(BigInteger.valueOf(12).negate(), result)
  }

  private fun mockWorkloadPoints(
    standardCourtReportWeighting: BigInteger = BigInteger.ZERO,
    fastCourtReportWeighting: BigInteger = BigInteger.ZERO,
    isT2A: Boolean = false,
    paroleReportWeighting: Int = 0,
    paroleEnabled: Boolean = true,
    licenseARMAssessmentWeighting: BigInteger = BigInteger.ZERO,
    communityARMAssessmentWeighting: BigInteger = BigInteger.ZERO,
  ): WorkloadPointsEntity {
    val workloadPoints = WorkloadPointsEntity(
      null,
      ZonedDateTime.now(),
      ZonedDateTime.now(),
      isT2A,
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      standardCourtReportWeighting,
      fastCourtReportWeighting,
      paroleReportWeighting,
      paroleEnabled,
      licenseARMAssessmentWeighting,
      communityARMAssessmentWeighting,
    )

    return workloadPoints
  }
}
