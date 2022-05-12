package uk.gov.justice.digital.hmpps.hmppsworkload.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsworkload.client.CommunityApiClient
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.PersonManagerDetails
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.repository.PersonManagerRepository
import uk.gov.justice.digital.hmpps.hmppsworkload.mapper.GradeMapper
import java.util.Optional
import java.util.UUID

@Service
class JpaBasedGetPersonManager(
  private val personManagerRepository: PersonManagerRepository,
  private val communityApiClient: CommunityApiClient,
  private val gradeMapper: GradeMapper
) : GetPersonManager {
  override fun findById(id: UUID): PersonManagerDetails? = personManagerRepository.findByUuid(id)?.let { entity ->
    val staff = communityApiClient.getStaffById(entity.staffId).map { Optional.of(it) }.onErrorResume { Mono.just(Optional.empty()) }.block()!!
    PersonManagerDetails.from(entity, gradeMapper.deliusToStaffGrade(staff.map { it.staffGrade?.code }.orElse(null)), staff.orElse(null))
  }
}
