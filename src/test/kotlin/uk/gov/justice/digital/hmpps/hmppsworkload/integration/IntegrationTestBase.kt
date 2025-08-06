package uk.gov.justice.digital.hmpps.hmppsworkload.integration

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.HmppsAllocationMessage
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.event.HmppsMessage
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.domain.WMTPointsWeightings
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.domain.WMTStaff
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.entity.CaseCategoryEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.entity.WMTCaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.entity.WMTWorkloadEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.entity.WorkloadPointsCalculationEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.entity.WorkloadReportEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.repository.CaseCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.repository.PduRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.repository.ReductionCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.repository.ReductionReasonRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.repository.RegionRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.repository.TiersRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.repository.WMTCaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.repository.WMTWorkloadRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.repository.WorkloadPointsCalculationRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.jpa.repository.WorkloadReportRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.AssessRisksNeedsApiExtension
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.TierApiExtension
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.mockserver.WorkforceAllocationsToDeliusExtension
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.OffenderManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.PduEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.RegionEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.TeamEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.WMTWorkloadOwnerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.AdjustmentReasonRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.CaseDetailsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.EventManagerAuditRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.EventManagerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.OffenderManagerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.PersonManagerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.ReductionsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.RequirementManagerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.TeamRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WMTCMSRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WMTCourtReportsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WMTWorkloadOwnerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WorkloadCalculationRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.WorkloadPointsRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.listener.HmppsOffenderEvent
import uk.gov.justice.digital.hmpps.hmppsworkload.service.AuditMessage
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import java.math.BigDecimal

@ExtendWith(
  AssessRisksNeedsApiExtension::class,
  TierApiExtension::class,
  WorkforceAllocationsToDeliusExtension::class,
  HmppsAuthApiExtension::class,
)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegrationTestBase {

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  protected lateinit var personManagerRepository: PersonManagerRepository

  @Autowired
  protected lateinit var eventManagerRepository: EventManagerRepository

  @Autowired
  protected lateinit var caseDetailsRepository: CaseDetailsRepository

  @Autowired
  protected lateinit var requirementManagerRepository: RequirementManagerRepository

  @Autowired
  protected lateinit var wmtCourtReportsRepository: WMTCourtReportsRepository

  @Autowired
  protected lateinit var wmtcmsRepository: WMTCMSRepository

  @Autowired
  protected lateinit var reductionsRepository: ReductionsRepository

  @Autowired
  protected lateinit var wmtWorkloadOwnerRepository: WMTWorkloadOwnerRepository

  @Autowired
  protected lateinit var teamRepository: TeamRepository

  @Autowired
  protected lateinit var offenderManagerRepository: OffenderManagerRepository

  @Autowired
  protected lateinit var adjustmentReasonRepository: AdjustmentReasonRepository

  @Autowired
  protected lateinit var workloadCalculationRepository: WorkloadCalculationRepository

  private val hmppsAllocationCompleteQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsallocationcompletequeue")
      ?: throw MissingQueueException("HmppsQueue hmppsallocationcompletequeue not found")
  }

  protected val allocationCompleteUrl by lazy {
    hmppsQueueService.findByQueueId("hmppsallocationcompletequeue")?.queueUrl
      ?: throw MissingQueueException("HmppsQueue allocationcompletequeue not found")
  }

  private val hmppsOffenderQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsoffenderqueue")
      ?: throw MissingQueueException("HmppsQueue hmppsoffenderqueue not found")
  }
  private val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsdomaintopic")
      ?: throw MissingQueueException("HmppsTopic hmppsdomaintopic not found")
  }
  private val offenderEventTopic by lazy {
    hmppsQueueService.findByTopicId("hmppsoffendertopic")
      ?: throw MissingQueueException("HmppsTopic hmppsoffendertopic not found")
  }

  private val tierCalculationQueue by lazy {
    hmppsQueueService.findByQueueId("tiercalcqueue")
      ?: throw MissingQueueException("HmppsQueue tiercalcqueue not found")
  }

  private val hmppsAuditQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsauditqueue")
      ?: throw MissingQueueException("HmppsQueue hmppsauditqueue not found")
  }
  private val hmppsReductionsCompletedQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsreductionscompletedqueue")
      ?: throw MissingQueueException("HmppsQueue hmppsreductionsCompletedqueue not found")
  }
  private val workloadCalculationQueue by lazy {
    hmppsQueueService.findByQueueId("workloadcalculationqueue")
      ?: throw MissingQueueException("HmppsQueue workloadcalculationqueue not found")
  }
  protected val hmppsExtractPlacedQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsextractplacedqueue")
      ?: throw MissingQueueException("HmppsQueue hmppsextractplacedqueue not found")
  }

  private val workloadPrisonerQueue by lazy {
    hmppsQueueService.findByQueueId("workloadprisonerqueue")
      ?: throw MissingQueueException("HmppsQueue  workloadprisonerqueue not found")
  }

  protected val notificationQueue by lazy {
    hmppsQueueService.findByQueueId("hmppsnotificationqueue")
      ?: throw MissingQueueException("HmppsQueue  hmppsnotificationqueue not found")
  }

  private val allocationCompleteSqsClient by lazy { hmppsAllocationCompleteQueue.sqsClient }
  private val allocationCompleteSqsDlqClient by lazy { hmppsAllocationCompleteQueue.sqsDlqClient }

  private val hmppsOffenderSqsDlqClient by lazy { hmppsOffenderQueue.sqsDlqClient }
  protected val hmppsOffenderSqsClient by lazy { hmppsOffenderQueue.sqsClient }

  protected val hmppsOffenderSnsClient by lazy { offenderEventTopic.snsClient }
  protected val hmppsOffenderTopicArn by lazy { offenderEventTopic.arn }

  protected val hmppsDomainSnsClient by lazy { domainEventsTopic.snsClient }
  protected val hmppsDomainTopicArn by lazy { domainEventsTopic.arn }

  private val tierCalculationSqsDlqClient by lazy { tierCalculationQueue.sqsDlqClient }
  protected val tierCalculationSqsClient by lazy { tierCalculationQueue.sqsClient }

  private val workloadCalculationSqsDlqClient by lazy { workloadCalculationQueue.sqsDlqClient }
  protected val workloadCalculationSqsClient by lazy { workloadCalculationQueue.sqsClient }

  protected val hmppsAuditQueueClient by lazy { hmppsAuditQueue.sqsClient }

  protected val hmppsReductionsCompletedClient by lazy { hmppsReductionsCompletedQueue.sqsClient }

  protected val hmppsExtractPlacedClient by lazy { hmppsExtractPlacedQueue.sqsClient }
  private val hmppsExtractPlacedDlqClient by lazy { hmppsExtractPlacedQueue.sqsDlqClient }

  private val workloadPrisonerSqsDlqClient by lazy { workloadPrisonerQueue.sqsDlqClient }
  protected val workloadPrisonerSqsClient by lazy { workloadPrisonerQueue.sqsClient }

  protected val notificationSqsClient by lazy { notificationQueue.sqsClient }
  private val notificationSqsDlqClient by lazy { notificationQueue.sqsDlqClient }

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  protected lateinit var tiersRepository: TiersRepository

  @Autowired
  protected lateinit var wmtCaseDetailsRepository: WMTCaseDetailsRepository

  @Autowired
  protected lateinit var workloadPointsCaseDetailsRepository: WorkloadPointsCalculationRepository

  @Autowired
  protected lateinit var wmtWorkloadRepository: WMTWorkloadRepository

  @Autowired
  protected lateinit var workloadReportRepository: WorkloadReportRepository

  @Autowired
  protected lateinit var reductionReasonRepository: ReductionReasonRepository

  @Autowired
  protected lateinit var reductionCategoryRepository: ReductionCategoryRepository

  @Autowired
  protected lateinit var regionRepository: RegionRepository

  @Autowired
  protected lateinit var pduRepository: PduRepository

  @Autowired
  protected lateinit var caseCategoryRepository: CaseCategoryRepository

  @Autowired
  protected lateinit var workloadPointsCalculationRepository: WorkloadPointsCalculationRepository

  @Autowired
  protected lateinit var workloadPointsRepository: WorkloadPointsRepository

  @Autowired
  protected lateinit var eventManagerAuditRepository: EventManagerAuditRepository

  @BeforeEach
  fun setupDependentServices() {
    personManagerRepository.deleteAll()
    eventManagerAuditRepository.deleteAll()
    eventManagerRepository.deleteAll()
    requirementManagerRepository.deleteAll()
    caseDetailsRepository.deleteAll()
    wmtCourtReportsRepository.deleteAll()
    wmtcmsRepository.deleteAll()
    reductionsRepository.deleteAll()
    adjustmentReasonRepository.deleteAll()
    allocationCompleteSqsClient.purgeQueue(
      PurgeQueueRequest.builder().queueUrl(hmppsAllocationCompleteQueue.queueUrl).build(),
    ).get()
    allocationCompleteSqsDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(hmppsAllocationCompleteQueue.dlqUrl!!).build()).get()
    hmppsOffenderSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(hmppsOffenderQueue.queueUrl).build()).get()
    hmppsOffenderSqsDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(hmppsOffenderQueue.dlqUrl!!).build()).get()
    workloadCalculationSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(workloadCalculationQueue.queueUrl).build()).get()
    workloadCalculationSqsDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(workloadCalculationQueue.dlqUrl!!).build()).get()
    workloadPrisonerSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(workloadPrisonerQueue.queueUrl).build()).get()
    workloadPrisonerSqsDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(workloadPrisonerQueue.dlqUrl!!).build()).get()
    hmppsExtractPlacedClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(hmppsExtractPlacedQueue.queueUrl).build()).get()
    hmppsExtractPlacedDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(hmppsExtractPlacedQueue.dlqUrl!!).build()).get()
    hmppsReductionsCompletedClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(hmppsReductionsCompletedQueue.queueUrl).build()).get()
    hmppsAuditQueueClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(hmppsAuditQueue.queueUrl).build()).get()
    notificationSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(notificationQueue.queueUrl).build()).get()
    notificationSqsDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(notificationQueue.dlqUrl!!).build()).get()
    tierCalculationSqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(tierCalculationQueue.queueUrl).build()).get()
    tierCalculationSqsDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(tierCalculationQueue.dlqUrl!!).build()).get()

    workloadCalculationRepository.deleteAll()
    clearWMT()
  }

  @AfterAll
  fun tearDownServer() {
    personManagerRepository.deleteAll()
    eventManagerAuditRepository.deleteAll()
    eventManagerRepository.deleteAll()
    requirementManagerRepository.deleteAll()
    wmtCourtReportsRepository.deleteAll()
    wmtcmsRepository.deleteAll()
    reductionsRepository.deleteAll()
    adjustmentReasonRepository.deleteAll()
    workloadCalculationRepository.deleteAll()
  }

  fun clearWMT() {
    tiersRepository.deleteAll()
    wmtCaseDetailsRepository.deleteAll()
    caseCategoryRepository.deleteAll()
    workloadPointsCaseDetailsRepository.deleteAll()
    workloadPointsCalculationRepository.deleteAll()
    wmtWorkloadRepository.deleteAll()
    workloadReportRepository.deleteAll()
    reductionsRepository.deleteAll()
    reductionReasonRepository.deleteAll()
    reductionCategoryRepository.deleteAll()
    wmtWorkloadOwnerRepository.deleteAll()
    offenderManagerRepository.deleteAll()
    teamRepository.deleteAll()
    pduRepository.deleteAll()
    regionRepository.deleteAll()
  }

  protected fun setupCurrentWmtStaff(staffCode: String, teamCode: String, totalFilteredCustodyCases: Int = 20): WMTStaff {
    val region = regionRepository.findByCode("REGION1") ?: regionRepository.save(
      RegionEntity(
        code = "REGION1",
        description = "Region 1",
      ),
    )
    val pdu = pduRepository.findByCode("PDU1") ?: pduRepository.save(
      PduEntity(
        code = "PDU1",
        description = "Local Delivery Unit (Actually a Probation Delivery Unit)",
        region = region,
      ),
    )
    val team = teamRepository.findByCode(teamCode) ?: teamRepository.save(
      TeamEntity(
        code = teamCode,
        description = "Team 1",
        ldu = pdu,
      ),
    )
    val offenderManager = offenderManagerRepository.findByCode(staffCode) ?: offenderManagerRepository.save(
      OffenderManagerEntity(
        code = staffCode,
        forename = "Jane",
        surname = "Doe",
        typeId = 1,
      ),
    )
    val workloadOwner = wmtWorkloadOwnerRepository.save(
      WMTWorkloadOwnerEntity(
        offenderManager = offenderManager,
        team = team,
        contractedHours = BigDecimal.valueOf(37.5),
      ),
    )
    val workloadReport = workloadReportRepository.findByEffectiveFromIsNotNullAndEffectiveToIsNull()
      ?: workloadReportRepository.save((WorkloadReportEntity()))
    val wmtWorkload = wmtWorkloadRepository.save(
      WMTWorkloadEntity(
        workloadOwner = workloadOwner,
        workloadReport = workloadReport,
        totalFilteredCommunityCases = 10,
        totalFilteredCustodyCases = totalFilteredCustodyCases,
        totalFilteredLicenseCases = 5,
        institutionalReportsDueInNextThirtyDays = 5,
        totalFilteredCases = 35,
      ),
    )
    val currentWmtPointsWeightings = getWmtWorkloadWeightings()
    val workloadPointsCalculation = workloadPointsCalculationRepository.save(
      WorkloadPointsCalculationEntity(
        workloadReport = workloadReport,
        workloadPoints = currentWmtPointsWeightings.normalWeightings,
        t2aWorkloadPoints = currentWmtPointsWeightings.t2aWeightings,
        workload = wmtWorkload,
        totalPoints = 500,
        availablePoints = 1000,
      ),
    )
    return WMTStaff(offenderManager, team, workloadOwner, wmtWorkload, workloadPointsCalculation)
  }

  protected fun setupWmtManagedCase(wmtStaff: WMTStaff, tier: Tier, crn: String, caseType: CaseType) {
    val tierCategory = setupWmtCaseCategoryTier(tier)
    wmtCaseDetailsRepository.save(
      WMTCaseDetailsEntity(
        workload = wmtStaff.workload,
        crn = crn,
        tierCategory = tierCategory,
        caseType = caseType,
        teamCode = wmtStaff.team.code,
      ),
    )
  }

  protected fun setupWmtCaseCategoryTier(tier: Tier): CaseCategoryEntity = caseCategoryRepository.findByCategoryName(tier.name)
    ?: caseCategoryRepository.save(CaseCategoryEntity(categoryName = tier.name, categoryId = tier.ordinal))

  protected fun setupWmtUntiered(): CaseCategoryEntity = caseCategoryRepository.findByCategoryName("Untiered")
    ?: caseCategoryRepository.save(CaseCategoryEntity(categoryName = "Untiered", categoryId = 0))

  protected fun getWmtWorkloadWeightings(): WMTPointsWeightings = WMTPointsWeightings(
    workloadPointsRepository.findFirstByIsT2AAndEffectiveToIsNullOrderByEffectiveFromDesc(true),
    workloadPointsRepository.findFirstByIsT2AAndEffectiveToIsNullOrderByEffectiveFromDesc(false),
  )

  internal fun HttpHeaders.authToken(roles: List<String> = emptyList(), subject: String = "SOME_USER") {
    this.setBearerAuth(
      jwtAuthHelper.createJwt(
        subject = subject,
        roles = roles,
        clientId = "some-client",
      ),
    )
  }

  protected fun noMessagesOnOffenderEventsQueue() {
    numberOfMessagesCurrentlyOnQueue(hmppsOffenderSqsClient, hmppsOffenderQueue.queueUrl, 0)
  }

  protected fun noMessagesOnExtractPlacedQueue() {
    numberOfMessagesCurrentlyOnQueue(hmppsExtractPlacedClient, hmppsExtractPlacedQueue.queueUrl, 0)
  }

  protected fun noMessagesOnWorkloadCalculationEventsQueue() {
    numberOfMessagesCurrentlyOnQueue(workloadCalculationSqsClient, workloadCalculationQueue.queueUrl, 0)
  }

  protected fun noMessagesOnWorkloadPrisonerQueue() {
    numberOfMessagesCurrentlyOnQueue(workloadPrisonerSqsClient, workloadPrisonerQueue.queueUrl, 0)
  }

  protected fun noMessagesOnWorkloadPrisonerDLQ() {
    numberOfMessagesCurrentlyOnQueue(workloadPrisonerSqsDlqClient!!, workloadPrisonerQueue.dlqUrl!!, 0)
  }

  protected fun noMessagesOnWorkloadCalculationEventsDLQ() {
    numberOfMessagesCurrentlyOnQueue(workloadCalculationSqsDlqClient!!, workloadCalculationQueue.dlqUrl!!, 0)
  }

  protected fun oneMessageOnWorkloadCalculationDeadLetterQueue() {
    numberOfMessagesCurrentlyOnQueue(workloadCalculationSqsDlqClient!!, workloadCalculationQueue.dlqUrl!!, 1)
  }

  protected fun noMessagesOnOffenderEventsDLQ() {
    numberOfMessagesCurrentlyOnQueue(hmppsOffenderSqsDlqClient!!, hmppsOffenderQueue.dlqUrl!!, 0)
  }

  protected fun noMessagesOnExtractPlacedDLQ() {
    numberOfMessagesCurrentlyOnQueue(hmppsExtractPlacedDlqClient!!, hmppsExtractPlacedQueue.dlqUrl!!, 0)
  }

  protected fun noMessagesOnNotificationQueue() {
    numberOfMessagesCurrentlyOnQueue(notificationSqsClient, notificationQueue.queueUrl, 0)
  }

  protected fun messagesOnNotificationQueue() {
    numberOfMessagesCurrentlyOnQueue(notificationSqsClient, notificationQueue.queueUrl, 1)
  }

  protected fun noMessagesOnNotificationQueueDLQ() {
    numberOfMessagesCurrentlyOnQueue(notificationSqsClient, notificationQueue.queueUrl, 0)
  }

  protected fun messagesOnNotificationQueueDLQ() {
    numberOfMessagesCurrentlyOnQueue(notificationSqsDlqClient!!, notificationQueue.dlqUrl!!, 1)
  }

  protected fun offenderEvent(crn: String) = HmppsOffenderEvent(crn)

  protected fun jsonString(any: Any) = objectMapper.writeValueAsString(any) as String

  protected fun expectWorkloadAllocationCompleteMessages(crn: String): Map<String, HmppsMessage<HmppsAllocationMessage>> {
    numberOfMessagesCurrentlyOnQueue(allocationCompleteSqsClient, allocationCompleteUrl, 3)
    val changeEvents = getAllAllocationMessages()
    changeEvents.forEach { changeEvent ->
      Assertions.assertEquals(crn, changeEvent.personReference.identifiers.first { it.type == "CRN" }.value)
    }
    val numberOfPersonAllocationMessages = changeEvents.count { it.eventType == "person.community.manager.allocated" }
    Assertions.assertEquals(1, numberOfPersonAllocationMessages)

    val numberOfEventAllocationMessages = changeEvents.count { it.eventType == "event.manager.allocated" }
    Assertions.assertEquals(1, numberOfEventAllocationMessages)

    val numberOfRequirementAllocationMessages = changeEvents.count { it.eventType == "requirement.manager.allocated" }
    Assertions.assertEquals(1, numberOfRequirementAllocationMessages)

    return changeEvents.associateBy { it.eventType }
  }

  private fun getAllAllocationMessages(): List<HmppsMessage<HmppsAllocationMessage>> {
    val messages = ArrayList<HmppsMessage<HmppsAllocationMessage>>()
    while (getNumberOfMessagesCurrentlyOnQueue(allocationCompleteSqsClient, allocationCompleteUrl) != 0) {
      val message = allocationCompleteSqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(allocationCompleteUrl).build())
      messages.addAll(
        message.get().messages().map {
          allocationCompleteSqsClient.deleteMessage(
            DeleteMessageRequest.builder()
              .queueUrl(allocationCompleteUrl)
              .receiptHandle(it.receiptHandle())
              .build(),
          ).get()
          val sqsMessage = objectMapper.readValue(it.body(), SQSMessage::class.java)
          val personAllocationMessageType = object : TypeReference<HmppsMessage<HmppsAllocationMessage>>() {}
          objectMapper.readValue(sqsMessage.Message, personAllocationMessageType)
        },
      )
    }
    return messages
  }

  protected fun verifyAuditMessageOnQueue(): Boolean = getNumberOfMessagesCurrentlyOnQueue(hmppsAuditQueueClient, hmppsAuditQueue.queueUrl) == 1

  protected fun getAuditMessages(): AuditMessage {
    val message = hmppsAuditQueueClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(hmppsAuditQueue.queueUrl).build())
    return message.get().messages().map {
      hmppsAuditQueueClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(hmppsAuditQueue.queueUrl).receiptHandle(it.receiptHandle()).build())
      val auditDataType = object : TypeReference<AuditMessage>() {}
      objectMapper.readValue(it.body(), auditDataType)
    }.first()
  }

  protected fun verifyReductionsCompletedOnQueue(): Boolean = getNumberOfMessagesCurrentlyOnQueue(hmppsReductionsCompletedClient, hmppsReductionsCompletedQueue.queueUrl) == 1

  protected fun getReductionsCompletedMessages(): HmppsMessage<JsonNode> {
    val message = hmppsReductionsCompletedClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(hmppsReductionsCompletedQueue.queueUrl).build())
    return message.get().messages().map {
      hmppsReductionsCompletedClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(hmppsReductionsCompletedQueue.queueUrl).receiptHandle(it.receiptHandle()).build())
      val reductionsCompletedMessageDataType = object : TypeReference<HmppsMessage<JsonNode>>() {}
      objectMapper.readValue(it.body(), reductionsCompletedMessageDataType)
    }.first()
  }
}

data class SQSMessage(
  val Message: String,
  val MessageId: String,
)
