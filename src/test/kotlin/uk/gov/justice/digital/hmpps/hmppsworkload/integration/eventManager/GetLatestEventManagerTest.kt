package uk.gov.justice.digital.hmpps.hmppsworkload.integration.eventManager

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.EventDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.StaffIdentifier
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.CaseDetailsEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity.EventManagerEntity
import uk.gov.justice.digital.hmpps.hmppsworkload.service.staff.JpaBasedGetEventManager

class GetLatestEventManagerTest : IntegrationTestBase() {

  @Autowired
  private lateinit var getEventManager: JpaBasedGetEventManager

  @Test
  fun `must return latest case allocated when last allocated case requested`() {
    val staffCode = "OM1"
    val teamCode = "T1"

    val savedEntity = eventManagerRepository.save(
      EventManagerEntity(
        crn = "CRN6634",
        staffCode = staffCode,
        teamCode = teamCode,
        createdBy = "createdBy",
        isActive = true,
        eventNumber = 1,
        spoStaffCode = "SP2",
        spoName = "Fred flintstone",
      ),
    )
    val eventManagerEntity = eventManagerRepository.findByIdOrNull(savedEntity.id!!)!!
    val realtimeCase = EventDetails(Tier.A, CaseType.LICENSE, eventManagerEntity.crn, eventManagerEntity.createdDate!!)
    // realtime
    caseDetailsRepository.save(CaseDetailsEntity(realtimeCase.crn, realtimeCase.tier, false, realtimeCase.type, "Jane", "Doe"))
    val result = getEventManager.findLatestByStaffAndTeam(StaffIdentifier(staffCode, teamCode))

    Assertions.assertEquals(realtimeCase, result)
  }

  @Test
  fun `must only return when case details are known`() {
    val staffCode = "OM1"
    val teamCode = "T1"
    eventManagerRepository.save(
      EventManagerEntity(
        crn = "CRN6634",
        staffCode = staffCode,
        teamCode = teamCode,
        createdBy = "createdBy",
        isActive = true,
        eventNumber = 1,
        spoStaffCode = "SP2",
        spoName = "Fred flintstone",
      ),
    )

    Assertions.assertNull(getEventManager.findLatestByStaffAndTeam(StaffIdentifier(staffCode, teamCode)))
  }

  @Test
  fun `must return latest case when multiple are allocated`() {
    val staffCode = "OM1"
    val teamCode = "T1"

    val eventManagerEntity = eventManagerRepository.save(
      EventManagerEntity(
        crn = "CRN6634",
        staffCode = staffCode,
        teamCode = teamCode,
        createdBy = "createdBy",
        isActive = true,
        eventNumber = 1,
        spoStaffCode = "SP2",
        spoName = "Fred flintstone",
      ),
    )

    caseDetailsRepository.save(CaseDetailsEntity(eventManagerEntity.crn, Tier.A, false, CaseType.LICENSE, "Jane", "Doe"))

    val savedEntity = eventManagerRepository.save(
      EventManagerEntity(
        crn = "CRN9977",
        staffCode = staffCode,
        teamCode = teamCode,
        createdBy = "createdBy",
        isActive = true,
        eventNumber = 1,
        spoStaffCode = "SP2",
        spoName = "Fred flintstone",
      ),
    )
    val latestEventManagerEntity = eventManagerRepository.findByIdOrNull(savedEntity.id!!)!!
    val realtimeCase = EventDetails(Tier.C, CaseType.COMMUNITY, latestEventManagerEntity.crn, latestEventManagerEntity.createdDate!!)
    // realtime
    caseDetailsRepository.save(CaseDetailsEntity(latestEventManagerEntity.crn, realtimeCase.tier, false, realtimeCase.type, "Jane", "Doe"))

    val result = getEventManager.findLatestByStaffAndTeam(StaffIdentifier(staffCode, teamCode))

    Assertions.assertEquals(realtimeCase, result)
  }

  @Test
  fun `must not return event manager if allocated to another staff member`() {
    val staffCode = "OM1"
    val teamCode = "T1"

    val savedEntity = eventManagerRepository.save(
      EventManagerEntity(
        crn = "CRN6634",
        staffCode = staffCode,
        teamCode = teamCode,
        createdBy = "createdBy",
        isActive = false,
        eventNumber = 1,
        spoStaffCode = "SP2",
        spoName = "Fred flintstone",
      ),
    )
    val eventManagerEntity = eventManagerRepository.findByIdOrNull(savedEntity.id!!)!!
    val realtimeCase = EventDetails(Tier.A, CaseType.LICENSE, eventManagerEntity.crn, eventManagerEntity.createdDate!!)
    caseDetailsRepository.save(CaseDetailsEntity(realtimeCase.crn, realtimeCase.tier, false, realtimeCase.type, "Jane", "Doe"))

    eventManagerRepository.save(
      EventManagerEntity(
        crn = eventManagerEntity.crn,
        staffCode = "ADIFFEENTCODE",
        teamCode = teamCode,
        createdBy = "createdBy",
        isActive = true,
        eventNumber = 1,
        spoStaffCode = "SP2",
        spoName = "Fred flintstone",
      ),
    )

    Assertions.assertNull(getEventManager.findLatestByStaffAndTeam(StaffIdentifier(staffCode, teamCode)))
  }
}
