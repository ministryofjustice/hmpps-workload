package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsworkload.client.AssessRisksNeedsApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.WorkforceAllocationsToDeliusApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.AllocationDemandDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.InitialAppointment
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.OffenceDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Requirement
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskOGRS
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskPredictor
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskSummary
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.utils.DateUtils
import uk.gov.justice.digital.hmpps.hmppsworkload.utils.capitalize
import uk.gov.service.notify.NotificationClientApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.collections.HashSet

const val SCORE_UNAVAILABLE = "Score Unavailable"
private const val NOT_APPLICABLE = "N/A"
private const val REFERENCE_ID = "referenceId"
private const val CRN = "crn"

@Service
class NotificationService(
  private val notificationClient: NotificationClientApi,
  @Value("\${application.notify.allocation.template}") private val allocationTemplateId: String,
  @Value("\${application.notify.allocation.laoTemplate}") private val allocationTemplateLAOId: String,
  @Qualifier("assessRisksNeedsClientUserEnhanced") private val assessRisksNeedsApiClient: AssessRisksNeedsApiClient,
  private val sqsSuccessPublisher: SqsSuccessPublisher,
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Suppress("LongParameterList", "LongMethod")
  suspend fun notifyAllocation(allocationDemandDetails: AllocationDemandDetails, allocateCase: AllocateCase, caseDetails: CaseDetailsEntity): NotificationMessageResponse {
    val emailReferenceId = UUID.randomUUID().toString()
    val notifyData = getNotifyData(allocateCase.crn)
    val parameters: Map<String, Any>
    val templateId: String
    if (allocateCase.laoCase) {
      templateId = allocationTemplateLAOId
      parameters = mapOf(
        "officer_name" to allocationDemandDetails.staff.name.getCombinedName(),
      ).plus(getLoggedInUserParameters(allocationDemandDetails.allocatingStaff))
        .plus(CRN to allocationDemandDetails.crn)
    } else {
      templateId = allocationTemplateId
      parameters = mapOf(
        "officer_name" to allocationDemandDetails.staff.name.getCombinedName(),
        "induction_statement" to mapInductionAppointment(allocationDemandDetails.initialAppointment, caseDetails.type),
        "requirements" to mapRequirements(allocationDemandDetails.activeRequirements),
      ).plus(getRiskParameters(notifyData.riskSummary, notifyData.riskPredictors, allocationDemandDetails.ogrs))
        .plus(getConvictionParameters(allocationDemandDetails))
        .plus(getPersonOnProbationParameters(allocationDemandDetails.name.getCombinedName(), allocateCase))
        .plus(getLoggedInUserParameters(allocationDemandDetails.allocatingStaff))
    }
    logProbationEstateDetails(allocationDemandDetails.allocatingStaff.code, allocationDemandDetails.crn, allocationDemandDetails.staff.code)
    val emailTo = HashSet(allocateCase.emailTo ?: emptySet())
    emailTo.add(allocationDemandDetails.staff.email!!)
    if (allocateCase.sendEmailCopyToAllocatingOfficer) emailTo.add(allocationDemandDetails.allocatingStaff.email)
    MDC.put(REFERENCE_ID, emailReferenceId)
    MDC.put(CRN, caseDetails.crn)
    log.info("Email request sent to Notify for crn: ${caseDetails.crn} with reference ID: $emailReferenceId")
    MDC.remove(REFERENCE_ID)
    MDC.remove(CRN)
    sqsSuccessPublisher.sendNotification(
      NotificationEmail(
        emailTo = emailTo,
        emailTemplate = templateId,
        emailReferenceId = emailReferenceId,
        emailParameters = parameters,
      ),
    )
    return NotificationMessageResponse(templateId, emailReferenceId, emailTo)
  }

  class NotificationInvalidSenderException(emailRecipient: String, cause: Throwable) : Exception("Unable to deliver to recipient $emailRecipient", cause)

  private fun getLoggedInUserParameters(loggedInUser: StaffMember): Map<String, Any> = mapOf(
    "allocatingOfficerName" to loggedInUser.name.getCombinedName(),
    "allocatingOfficerGrade" to loggedInUser.getGrade(),
  )

  private suspend fun logProbationEstateDetails(loggedInUser: String, crn: String, allocatedUser: String ) {
    val loggedInTeams = workforceAllocationsToDeliusApiClient.getDeliusAllowedTeamInfo(loggedInUser).map { it.code }
    val allocatedTeams = workforceAllocationsToDeliusApiClient.getDeliusAllowedTeamInfo(allocatedUser).map { it.code }
    MDC.put(CRN, crn)
    MDC.put("ProbationEstateDetails", "Allocating PE Data")
    log.info("Allocating PE Data for crn: $crn, loggedInUser: $loggedInUser, allocatedUser: $allocatedUser, loggedInTeams: $loggedInTeams, allocatedTeams: $allocatedTeams")
    MDC.remove(CRN)
    MDC.remove("ProbationEstateDetails")
  }

  private fun getPersonOnProbationParameters(name: String, allocateCase: AllocateCase): Map<String, Any> = mapOf(
    "case_name" to name,
    "crn" to allocateCase.crn,
    "notes" to allocateCase.allocationJustificationNotes.toString(),
  )

  private fun getConvictionParameters(allocationDemandDetails: AllocationDemandDetails): Map<String, Any> {
    val sentenceDate = allocationDemandDetails.sentence.date.withZoneSameInstant(ZoneId.systemDefault()).format(DateUtils.notifyDateFormat)
    log.info("Sentence date: {}", sentenceDate)
    return mapOf(
      "court_name" to allocationDemandDetails.court.name,
      "sentence_date" to sentenceDate,
      "offences" to mapOffences(allocationDemandDetails.offences),
      "order" to "${allocationDemandDetails.sentence.description} (${allocationDemandDetails.sentence.length})",
    )
  }

  private fun getRiskParameters(riskSummary: RiskSummary?, riskPredictors: List<RiskPredictor>, assessment: RiskOGRS?): Map<String, Any> {
    val latestRiskPredictor =
      riskPredictors.filter { riskPredictor -> riskPredictor.rsrScoreLevel != null && riskPredictor.rsrPercentageScore != null }
        .maxByOrNull { riskPredictor -> riskPredictor.completedDate ?: LocalDateTime.MIN }
    val rsrLevel = latestRiskPredictor?.rsrScoreLevel?.capitalize() ?: SCORE_UNAVAILABLE
    val rsrPercentage = latestRiskPredictor?.rsrPercentageScore?.toString() ?: NOT_APPLICABLE
    val rosh = riskSummary?.overallRiskLevel?.capitalize() ?: SCORE_UNAVAILABLE
    val ogrsLevel = assessment?.getOgrsLevel() ?: SCORE_UNAVAILABLE
    val ogrsPercentage = assessment?.score?.toString() ?: NOT_APPLICABLE
    return mapOf(
      "rosh" to rosh,
      "rsrLevel" to rsrLevel,
      "rsrPercentage" to rsrPercentage,
      "ogrsLevel" to ogrsLevel,
      "ogrsPercentage" to ogrsPercentage,
    )
  }

  private fun mapInductionAppointment(initialAppointment: InitialAppointment?, caseType: CaseType): String {
    return when (caseType) {
      CaseType.CUSTODY -> "no initial appointment needed"
      else -> {
        if (initialAppointment != null) {
          return if (ChronoUnit.DAYS.between(LocalDate.now(), initialAppointment.date) >= 0) {
            "their initial appointment is scheduled for ${initialAppointment.date.format(DateUtils.notifyDateFormat)}"
          } else {
            "their initial appointment was scheduled for ${initialAppointment.date.format(DateUtils.notifyDateFormat)}"
          }
        }
        return "no date found for the initial appointment, please check with your team"
      }
    }
  }

  private fun mapOffences(offences: List<OffenceDetails>): List<String> = offences
    .map { offence -> offence.mainCategory }

  private fun mapRequirements(requirements: List<Requirement>): List<String> = requirements
    .map { requirement -> "${requirement.mainCategory}: ${requirement.subCategory ?: requirement.mainCategory} ${requirement.length}".trimEnd() }

  private suspend fun getNotifyData(crn: String): NotifyData {
    val riskSummary = assessRisksNeedsApiClient.getRiskSummary(crn)
    val riskPredictors = assessRisksNeedsApiClient.getRiskPredictors(crn)
    return NotifyData(riskSummary, riskPredictors)
  }
}

data class NotificationMessageResponse(
  val templateId: String,
  val referenceId: String,
  val email: Set<String>,
)

data class NotifyData(
  val riskSummary: RiskSummary?,
  val riskPredictors: List<RiskPredictor>,
)

data class NotificationEmail(
  val emailTo: Set<String>,
  val emailTemplate: String,
  val emailReferenceId: String,
  val emailParameters: Map<String, Any>,
)
