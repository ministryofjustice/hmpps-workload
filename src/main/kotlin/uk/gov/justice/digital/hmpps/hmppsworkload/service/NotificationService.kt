package uk.gov.justice.digital.hmpps.hmppsworkload.service

import io.micrometer.core.instrument.MeterRegistry
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
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.ReallocationDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.Requirement
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskOGRS
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskPredictor
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.RiskSummary
import uk.gov.justice.digital.hmpps.hmppsworkload.client.dto.StaffMember
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocateCase
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.ReallocateCase
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
private const val FAILED_ALLOCATION_COUNTER = "failed_allocation_notification"
private const val OFFICER_NAME = "officer_name"
private const val ALLOCATING_EMAIL = "allocating_email"
private const val PRACTITIONER_EMAIL = "practitioner_email"
private const val REQUIREMENTS = "requirements"
private const val OFFICER_GRADE = "officerGrade"
private const val INDUCTION_STATEMENT = "induction_statement"
private const val TIER = "tier"
private const val PREVIOUS_PRACTITIONER = "previous_pp"
private const val PREVIOUS_PRACTITIONER_GRADE = "previous_pp_grade"
private const val REALLOCATION_REASON = "reallocation_reason"
private const val FAILURE_TO_COMPLY_SINCE = "failure_to_comply_since"
private const val NEXT_APPOINTMENT = "next_appointment"
private const val OASYS_LAST_UPDATED = "oasys_last_updated"
private const val EMAIL_SENT_INFO = "Email request sent to Notify for crn: "
private const val FAILED_EMAIL_MSG = "Failed to send allocation email_to: {} email_from {} from_officer {}: for_crn {}"
private const val FAILED_REALLOCATION_EMAIL_MSG = "Failed to send reallocation email_to: {} email_from {} from_officer {}: for_crn {}"

@Service
@Suppress("LongParameterList", "LongMethod", "LargeClass", "TooManyFunctions")
class NotificationService(
  private val notificationClient: NotificationClientApi,
  @Value("\${application.notify.allocation.template}") private val allocationTemplateId: String,
  @Value("\${application.notify.allocation.laoTemplate}") private val allocationTemplateLAOId: String,
  @Value("\${application.notify.reallocation.new.template}") private val reallocationTemplateId: String,
  @Value("\${application.notify.reallocation.new.laoTemplate}") private val reallocationTemplateLAOId: String,
  @Value("\${application.notify.reallocation.previous.template}") private val reallocationPreviousTemplateId: String,
  @Value("\${application.notify.reallocation.previous.laoTemplate}") private val reallocationPreviousTemplateLAOId: String,
  @Qualifier("assessRisksNeedsClientUserEnhanced") private val assessRisksNeedsApiClient: AssessRisksNeedsApiClient,
  private val sqsSuccessPublisher: SqsSuccessPublisher,
  private val workforceAllocationsToDeliusApiClient: WorkforceAllocationsToDeliusApiClient,
  private val meterRegistry: MeterRegistry,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Suppress("LongParameterList", "LongMethod", "TooGenericExceptionCaught")
  suspend fun notifyAllocation(allocationDemandDetails: AllocationDemandDetails, allocateCase: AllocateCase, caseDetails: CaseDetailsEntity): NotificationMessageResponse {
    val emailReferenceId = UUID.randomUUID().toString()
    val notifyData = getNotifyData(allocateCase.crn)
    val parameters: Map<String, Any>
    val templateId: String
    if (allocateCase.laoCase) {
      templateId = allocationTemplateLAOId
      parameters = mapOf(
        OFFICER_NAME to allocationDemandDetails.staff.name.getCombinedName(),
        ALLOCATING_EMAIL to allocationDemandDetails.allocatingStaff.email!!,
        PRACTITIONER_EMAIL to allocationDemandDetails.staff.email!!,
      ).plus(getLoggedInUserParameters(allocationDemandDetails.allocatingStaff))
        .plus(CRN to allocationDemandDetails.crn)
    } else {
      templateId = allocationTemplateId
      parameters = mapOf(
        OFFICER_NAME to allocationDemandDetails.staff.name.getCombinedName(),
        INDUCTION_STATEMENT to mapInductionAppointment(allocationDemandDetails.initialAppointment, caseDetails.type),
        REQUIREMENTS to mapRequirements(allocationDemandDetails.activeRequirements),
        ALLOCATING_EMAIL to allocationDemandDetails.allocatingStaff.email!!,
        PRACTITIONER_EMAIL to allocationDemandDetails.staff.email!!,
      ).plus(getRiskParameters(notifyData.riskSummary, notifyData.riskPredictors, allocationDemandDetails.ogrs))
        .plus(getConvictionParameters(allocationDemandDetails))
        .plus(getPersonOnProbationParameters(allocationDemandDetails.name.getCombinedName(), allocateCase.crn, allocateCase.allocationJustificationNotes))
        .plus(getLoggedInUserParameters(allocationDemandDetails.allocatingStaff))
        .plus(CRN to allocationDemandDetails.crn)
    }
    logProbationEstateDetails(allocationDemandDetails.allocatingStaff.code, allocationDemandDetails.crn, allocationDemandDetails.staff.code)
    val emailTo = HashSet(allocateCase.emailTo ?: emptySet())
    emailTo.add(allocationDemandDetails.staff.email!!)
    if (allocateCase.sendEmailCopyToAllocatingOfficer) emailTo.add(allocationDemandDetails.allocatingStaff.email)

    sendNotification(templateId, emailReferenceId, parameters, caseDetails.crn, allocationDemandDetails, emailTo)

    return NotificationMessageResponse(templateId, emailReferenceId, emailTo)
  }

  @Suppress("LongParameterList", "LongMethod", "TooGenericExceptionCaught")
  suspend fun notifyReallocation(allocationDemandDetails: AllocationDemandDetails, allocateCase: ReallocateCase, tier: String?, reallocationDetail: ReallocationDetails): NotificationMessageResponse {
    val response = notifyReallocationNewPractitioner(allocationDemandDetails, allocateCase, tier, reallocationDetail)

    if (allocateCase.emailPreviousOfficer) {
      notifyReallocationPreviousPractitioner(allocationDemandDetails, allocateCase, tier, reallocationDetail)
    }

    return response
  }

  @Suppress("LongParameterList", "LongMethod", "TooGenericExceptionCaught")
  suspend fun notifyReallocationNewPractitioner(allocationDemandDetails: AllocationDemandDetails, allocateCase: ReallocateCase, tier: String?, reallocationDetail: ReallocationDetails): NotificationMessageResponse {
    val emailReferenceId = UUID.randomUUID().toString()
    val notifyData = getNotifyData(allocateCase.crn)
    val parameters: Map<String, Any>
    val templateId: String
    if (allocateCase.laoCase) {
      templateId = reallocationTemplateLAOId
      parameters = mapOf(
        OFFICER_NAME to allocationDemandDetails.staff.name.getCombinedName(),
        ALLOCATING_EMAIL to allocationDemandDetails.allocatingStaff.email!!,
        PRACTITIONER_EMAIL to allocationDemandDetails.staff.email!!,
      ).plus(getLoggedInUserParameters(allocationDemandDetails.allocatingStaff))
        .plus(CRN to allocationDemandDetails.crn)
    } else {
      templateId = reallocationTemplateId
      parameters = mapOf(
        OFFICER_NAME to allocationDemandDetails.staff.name.getCombinedName(),
        OFFICER_GRADE to allocationDemandDetails.staff.getGrade(),
        REQUIREMENTS to mapRequirements(reallocationDetail.requirements),
        ALLOCATING_EMAIL to allocationDemandDetails.allocatingStaff.email!!,
        OFFICER_GRADE to allocationDemandDetails.staff.getGrade(),
        PREVIOUS_PRACTITIONER to reallocationDetail.previouslyManagedBy.name.getCombinedName(),
        PREVIOUS_PRACTITIONER_GRADE to reallocationDetail.previouslyManagedBy.getGrade(),
        REALLOCATION_REASON to reallocationDetail.reason,
        OASYS_LAST_UPDATED to (reallocationDetail.oasysLastUpdated ?: ""),
        NEXT_APPOINTMENT to (reallocationDetail.nextAppointment ?: ""),
        FAILURE_TO_COMPLY_SINCE to (reallocationDetail.failureToComply ?: ""),
        PRACTITIONER_EMAIL to allocationDemandDetails.staff.email!!,
        TIER to (tier ?: ""),
      ).plus(getRiskParameters(notifyData.riskSummary, notifyData.riskPredictors, allocationDemandDetails.ogrs))
        .plus(getConvictionParameters(allocationDemandDetails, reallocationDetail))
        .plus(getPersonOnProbationParameters(allocationDemandDetails.name.getCombinedName(), allocateCase.crn, allocateCase.reallocationNotes))
        .plus(getLoggedInUserParameters(allocationDemandDetails.allocatingStaff))
        .plus(CRN to allocationDemandDetails.crn)
    }
    logProbationEstateDetails(allocationDemandDetails.allocatingStaff.code, allocationDemandDetails.crn, allocationDemandDetails.staff.code)
    val emailTo = HashSet(allocateCase.emailTo ?: emptySet())
    emailTo.add(allocationDemandDetails.staff.email!!)

    sendNotification(templateId, emailReferenceId, parameters, allocationDemandDetails.crn, allocationDemandDetails, emailTo)

    return NotificationMessageResponse(templateId, emailReferenceId, emailTo)
  }

  @Suppress("LongParameterList", "LongMethod", "TooGenericExceptionCaught")
  suspend fun notifyReallocationPreviousPractitioner(allocationDemandDetails: AllocationDemandDetails, allocateCase: ReallocateCase, tier: String?, reallocationDetail: ReallocationDetails): NotificationMessageResponse {
    val emailReferenceId = UUID.randomUUID().toString()
    val notifyData = getNotifyData(allocateCase.crn)
    val parameters: Map<String, Any>
    val templateId: String
    if (allocateCase.laoCase) {
      templateId = reallocationPreviousTemplateLAOId
      parameters = mapOf(
        OFFICER_NAME to reallocationDetail.previouslyManagedBy.name.getCombinedName(),
        ALLOCATING_EMAIL to allocationDemandDetails.allocatingStaff.email!!,
        PRACTITIONER_EMAIL to reallocationDetail.previouslyManagedBy.email!!,
      ).plus(getLoggedInUserParameters(allocationDemandDetails.allocatingStaff))
        .plus(CRN to allocationDemandDetails.crn)
    } else {
      templateId = reallocationPreviousTemplateId
      parameters = mapOf(
        OFFICER_NAME to allocationDemandDetails.staff.name.getCombinedName(),
        OFFICER_GRADE to allocationDemandDetails.staff.getGrade(),
        REQUIREMENTS to mapRequirements(reallocationDetail.requirements),
        ALLOCATING_EMAIL to allocationDemandDetails.allocatingStaff.email!!,
        PRACTITIONER_EMAIL to reallocationDetail.previouslyManagedBy.email!!,
        OFFICER_GRADE to allocationDemandDetails.staff.getGrade(),
        PREVIOUS_PRACTITIONER to reallocationDetail.previouslyManagedBy.name.getCombinedName(),
        PREVIOUS_PRACTITIONER_GRADE to reallocationDetail.previouslyManagedBy.getGrade(),
        REALLOCATION_REASON to reallocationDetail.reason,
        OASYS_LAST_UPDATED to (reallocationDetail.oasysLastUpdated ?: ""),
        NEXT_APPOINTMENT to (reallocationDetail.nextAppointment ?: ""),
        FAILURE_TO_COMPLY_SINCE to (reallocationDetail.failureToComply ?: ""),
        TIER to allocationDemandDetails.crn,

      ).plus(getRiskParameters(notifyData.riskSummary, notifyData.riskPredictors, allocationDemandDetails.ogrs))
        .plus(getConvictionParameters(allocationDemandDetails, reallocationDetail))
        .plus(getPersonOnProbationParameters(allocationDemandDetails.name.getCombinedName(), allocateCase.crn, allocateCase.reallocationNotes))
        .plus(getLoggedInUserParameters(allocationDemandDetails.allocatingStaff))
        .plus(CRN to allocationDemandDetails.crn)
    }
    logProbationEstateDetails(allocationDemandDetails.allocatingStaff.code, allocationDemandDetails.crn, allocationDemandDetails.staff.code)
    val previousEmail = reallocationDetail.previouslyManagedBy.email!!
    val emailTo = HashSet<String>()
    emailTo.add(previousEmail)

    sendNotification(templateId, emailReferenceId, parameters, allocationDemandDetails.crn, allocationDemandDetails, emailTo)

    return NotificationMessageResponse(templateId, emailReferenceId, emailTo)
  }

  @Suppress("LongParameterList", "LongMethod", "TooGenericExceptionCaught")
  private fun sendNotification(templateId: String, emailReferenceId: String, parameters: Map<String, Any>, crn: String, allocationDemandDetails: AllocationDemandDetails, emailTo: HashSet<String>) {
    try {
      sqsSuccessPublisher.sendNotification(
        NotificationEmail(
          emailTo = emailTo,
          emailTemplate = templateId,
          emailReferenceId = emailReferenceId,
          emailParameters = parameters,
        ),
      )
      MDC.put(REFERENCE_ID, emailReferenceId)
      MDC.put(CRN, crn)
      log.info("Email request sent to Notify for crn: $crn with reference ID: $emailReferenceId")
    } catch (exception: Exception) {
      meterRegistry.counter(FAILED_ALLOCATION_COUNTER, "type", "email not send").increment()
      log.error(FAILED_REALLOCATION_EMAIL_MSG, emailTo, allocationDemandDetails.staff.email, allocationDemandDetails.staff.name.getCombinedName(), allocationDemandDetails.crn, exception.message)
    } finally {
      MDC.remove(REFERENCE_ID)
      MDC.remove(CRN)
    }
  }

  private fun getLoggedInUserParameters(loggedInUser: StaffMember): Map<String, Any> = mapOf(
    "allocatingOfficerName" to loggedInUser.name.getCombinedName(),
    "allocatingOfficerGrade" to loggedInUser.getGrade(),
  )

  private suspend fun logProbationEstateDetails(loggedInUser: String, crn: String, allocatedUser: String) {
    val loggedInTeams = workforceAllocationsToDeliusApiClient.getDeliusAllowedTeamInfo(loggedInUser).teams.map { it.code }
    val allocatedTeams = workforceAllocationsToDeliusApiClient.getDeliusAllowedTeamInfo(allocatedUser).teams.map { it.code }
    MDC.put(CRN, crn)
    MDC.put("ProbationEstateDetails", "Allocating PE Data")
    log.info("Allocating PE Data for crn: $crn, loggedInUser: $loggedInUser, allocatedUser: $allocatedUser, loggedInTeams: $loggedInTeams, allocatedTeams: $allocatedTeams")
    MDC.remove(CRN)
    MDC.remove("ProbationEstateDetails")
  }

  private fun getPersonOnProbationParameters(name: String, crn: String, notes: String?): Map<String, Any> = mapOf(
    "case_name" to name,
    "crn" to crn,
    "notes" to notes.toString(),
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

  private fun getConvictionParameters(allocationDemandDetails: AllocationDemandDetails, reallocationDetails: ReallocationDetails): Map<String, Any> {
    val sentenceDate = allocationDemandDetails.sentence.date.withZoneSameInstant(ZoneId.systemDefault()).format(DateUtils.notifyDateFormat)
    log.info("Sentence date: {}", sentenceDate)
    return mapOf(
      "court_name" to allocationDemandDetails.court.name,
      "sentence_date" to sentenceDate,
      "offences" to mapOffences(reallocationDetails.offences),
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
