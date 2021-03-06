package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "ldu", schema = "app")
data class LduEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column
  val code: String,

  @Column
  val description: String,

  @ManyToOne
  @JoinColumn(name = "region_id")
  val region: RegionEntity,
)
