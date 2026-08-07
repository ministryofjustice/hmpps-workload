package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Contact
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CourtReport
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CourtReportType
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WorkloadPointsEntity
import java.math.BigInteger

@Component
class WorkloadCalculator {
  fun getWorkloadPoints(
    workloadPointsElements: WorkloadPointsElements,
  ): BigInteger {
    val courtReportTotal = calculateCourtReportPointsTotal(workloadPointsElements.courtReports, workloadPointsElements.workloadPointsWeighting)
    val contactPerformedOutsideCaseloadTotal = sumContacts(workloadPointsElements.contactsPerformedOutsideCaseload, workloadPointsElements.contactTypeWeightings)
    val contactsPerformedByOthersTotal = sumContacts(workloadPointsElements.contactsPerformedByOthers, workloadPointsElements.contactTypeWeightings).negate()
    return courtReportTotal.add(contactPerformedOutsideCaseloadTotal).add(contactsPerformedByOthersTotal)
  }
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
