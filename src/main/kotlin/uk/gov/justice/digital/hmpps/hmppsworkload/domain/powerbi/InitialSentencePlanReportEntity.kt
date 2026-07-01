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
@Table(name = "initial_sentence_plan_reports")
data class InitialSentencePlanReportEntity @JsonCreator constructor(

  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column
  @NotNull
  val crn: String,

  @Column
  @NotNull
  val personOnProbation: String,

  @Column
  @NotNull
  val inductionDate: Date,

  @Column
  @NotNull
  val targetDate: Date,

  @Column
  @NotNull
  val actions: String,

  @Column
  @NotNull
  val rosh: String,

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
