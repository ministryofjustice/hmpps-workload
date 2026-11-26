package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Case
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Contact
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CourtReport
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CourtReportType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.BreakdownDataEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WorkloadCalculationEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WorkloadPointsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WorkloadCalculationRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WorkloadPointsRepository
import java.math.BigDecimal
import java.math.BigInteger

@Suppress("LongParameterList")
@Service
class WorkloadCalculationService(
  private val workloadCalculator: WorkloadCalculator,
  private val getCourtReports: WMTGetCourtReports,
  private val getContacts: WMTGetContacts,
  private val getContactTypeWeightings: GetContactTypeWeightings,
  private val workloadPointsRepository: WorkloadPointsRepository,
  private val weeklyHours: GetWeeklyHours,
  private val workloadCalculationRepository: WorkloadCalculationRepository,
  private val getCaseLoad: GetCombinedCaseload,
) {

  fun saveWorkloadCalculation(
    staffIdentifier: StaffIdentifier,
    staffGrade: String,
    availableHours: BigDecimal = getAvailableHours(
      staffIdentifier,
      staffGrade,
    ),
  ): WorkloadCalculationEntity = workloadCalculationRepository.save(calculate(staffIdentifier, staffGrade, availableHours))

  private fun getAvailableHours(
    staffIdentifier: StaffIdentifier,
    staffGrade: String,
  ): BigDecimal = this.weeklyHours.findWeeklyHours(staffIdentifier, staffGrade)

  private fun calculate(staffIdentifier: StaffIdentifier, staffGrade: String, availableHours: BigDecimal): WorkloadCalculationEntity {
    val workloadCalculationsElements = getWorkloadCalculationElements(staffIdentifier)

    return WorkloadCalculationEntity(
      availablePoints = calculateAvailablePoints(
        workloadCalculationsElements.workloadPointsElements.workloadPointsWeighting.getDefaultPointsAvailable(staffGrade),
        workloadCalculationsElements.workloadPointsElements.workloadPointsWeighting.getDefaultContractedHours(staffGrade),
        availableHours,
      ),
      workloadPoints = workloadCalculationsElements.workloadPoints,
      staffCode = staffIdentifier.staffCode,
      teamCode = staffIdentifier.teamCode,
      breakdownData = BreakdownDataEntity(
        getCourtReportCounts(workloadCalculationsElements.workloadPointsElements.courtReports, CourtReportType.STANDARD),
        getCourtReportCounts(workloadCalculationsElements.workloadPointsElements.courtReports, CourtReportType.FAST),
        getContactTypeCodeCounts(workloadCalculationsElements.workloadPointsElements.contactsPerformedOutsideCaseload),
        getContactTypeCodeCounts(workloadCalculationsElements.workloadPointsElements.contactsPerformedByOthers),
        workloadCalculationsElements.workloadPointsElements.contactTypeWeightings,
        workloadCalculationsElements.workloadPointsElements.cases.size,
        availableHours,
      ),
    )
  }

  private fun getWorkloadCalculationElements(staffIdentifier: StaffIdentifier): WorkloadCalculationElements {
    val workloadPointsElements = WorkloadPointsElements(
      getCaseLoad.getCases(staffIdentifier),
      getCourtReports.getCourtReports(staffIdentifier),
      getContacts.findContactsOutsideCaseload(staffIdentifier),
      getContacts.findContactsInCaseloadPerformedByOthers(staffIdentifier),
      getContactTypeWeightings.findAll(),
      workloadPointsRepository.findFirstByIsT2AAndEffectiveToIsNullOrderByEffectiveFromDesc(true),
      workloadPointsRepository.findFirstByIsT2AAndEffectiveToIsNullOrderByEffectiveFromDesc(false),
    )
    return WorkloadCalculationElements(
      workloadPointsElements,
      workloadCalculator.getWorkloadPoints(workloadPointsElements),
    )
  }
  private fun getCourtReportCounts(courtReports: List<CourtReport>, type: CourtReportType): Int = courtReports.count { it.type == type }

  private fun getContactTypeCodeCounts(contacts: List<Contact>): Map<String, Int> = contacts.groupingBy { it.typeCode }.eachCount()
}

data class WorkloadCalculationElements(
  val workloadPointsElements: WorkloadPointsElements,
  val workloadPoints: BigInteger,
)

data class WorkloadPointsElements(
  val cases: List<Case>,
  val courtReports: List<CourtReport>,
  val contactsPerformedOutsideCaseload: List<Contact>,
  val contactsPerformedByOthers: List<Contact>,
  val contactTypeWeightings: Map<String, Int>,
  val t2aWorkloadPoints: WorkloadPointsEntity,
  val workloadPointsWeighting: WorkloadPointsEntity,
)
