package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Case
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Contact
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CourtReport
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CourtReportType
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WorkloadPointsEntity
import java.math.BigInteger

@Component
class DefaultWorkloadCalculator : WorkloadCalculator {
  override fun getWorkloadPoints(
    cases: List<Case>,
    courtReports: List<CourtReport>,
    contactsPerformedOutsideCaseload: List<Contact>,
    contactsPerformedByOthers: List<Contact>,
    contactTypeWeightings: Map<String, Int>,
    t2aWorkloadPoints: WorkloadPointsEntity,
    workloadPoints: WorkloadPointsEntity
  ): BigInteger {

    val casePointTotal = calculateCaseTierPointsTotal(cases, t2aWorkloadPoints, workloadPoints)
    val courtReportTotal = calculateCourtReportPointsTotal(courtReports, workloadPoints)
    val contactPerformedOutsideCaseloadTotal = sumContacts(contactsPerformedOutsideCaseload, contactTypeWeightings)
    val contactsPerformedByOthersTotal = sumContacts(contactsPerformedByOthers, contactTypeWeightings).negate()
    return casePointTotal.add(courtReportTotal).add(contactPerformedOutsideCaseloadTotal).add(contactsPerformedByOthersTotal)
  }

  private fun calculateCaseTierPointsTotal(cases: List<Case>, t2aWorkloadPoints: WorkloadPointsEntity, workloadPoints: WorkloadPointsEntity): BigInteger = cases.map { case ->
    var caseWorkloadPoints = workloadPoints
    if (case.isT2A) {
      caseWorkloadPoints = t2aWorkloadPoints
    }
    val tierWeightings = caseWorkloadPoints.getTierPointsMap(case.type)
    tierWeightings[case.tier]!!
  }.fold(BigInteger.ZERO) { first, second -> first.add(second) }

  private fun calculateCourtReportPointsTotal(courtReports: List<CourtReport>, workloadPoints: WorkloadPointsEntity): BigInteger = courtReports.map { courtReport ->
    when (courtReport.type) {
      CourtReportType.FAST -> workloadPoints.fastCourtReportPoints
      CourtReportType.STANDARD -> workloadPoints.standardCourtReportPoints
    }
  }.fold(BigInteger.ZERO) { first, second -> first.add(second) }

  private fun sumContacts(contacts: List<Contact>, contactTypeWeightings: Map<String, Int>): BigInteger = contacts
    .map { contact ->
      contactTypeWeightings.getOrDefault(contact.typeCode, 0)
    }.fold(0) { first, second -> first + second }.toBigInteger()
}
