package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.AllocationReason
import java.math.BigInteger
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "REQUIREMENT_MANAGER")
@EntityListeners(AuditingEntityListener::class)
data class RequirementManagerEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column
  val uuid: UUID = UUID.randomUUID(),

  @Column
  val crn: String,

  @Column
  val requirementId: BigInteger,

  @Column
  val staffCode: String,

  @Column
  val teamCode: String,

  @Column
  var createdBy: String,

  @Column
  @CreatedDate
  var createdDate: ZonedDateTime? = null,

  @Column
  var isActive: Boolean,

  @Column
  var eventNumber: Int,

  @Column
  @Enumerated(EnumType.STRING)
  var allocationReason: AllocationReason?,
)
