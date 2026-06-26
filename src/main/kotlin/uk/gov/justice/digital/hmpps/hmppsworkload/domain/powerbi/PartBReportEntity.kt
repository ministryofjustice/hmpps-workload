package uk.gov.justice.digital.hmpps.hmppsworkload.domain.powerbi

import com.fasterxml.jackson.annotation.JsonCreator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import java.sql.Date

@Entity
@Table(name = "part_b_reports")
data class PartBReportEntity @JsonCreator constructor(

  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long,

  @Column
  @NotNull
  val crn: String,

  @Column
  @NotNull
  val personOnProbation: String,

  @Column
  @NotNull
  val returnToCustodyDate: Date,

  @Column
  @NotNull
  val targetDate: Date,

  @Column
  @NotNull
  val tasks: String,

  @Column
  @NotNull
  val actions: String,

  @Column
  val rarrType: String,

  @Column
  @NotNull
  val pdu: String,

  @Column
  @NotNull
  val team: String,

  @Column
  @NotNull
  val probationPractitioner: String,
)
