package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "PERSON_MANAGER")
@EntityListeners(AuditingEntityListener::class)
data class PersonManagerEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column
  val uuid: UUID = UUID.randomUUID(),

  @Column
  val crn: String,

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

)
