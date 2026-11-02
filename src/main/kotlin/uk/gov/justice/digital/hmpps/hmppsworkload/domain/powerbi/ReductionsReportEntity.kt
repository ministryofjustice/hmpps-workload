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
@Table(name = "upcoming_releases_reports")
data class ReductionsReportEntity @JsonCreator constructor(

  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column
  @NotNull
  val staffName: String,

  @Column
  @NotNull
  val staffId: String,

  @Column
  @NotNull
  val staffTeamKey: String,

  @Column
  @NotNull
  val reductionType: String,

  @Column
  @NotNull
  val reductionHours: Double,

  @Column
  @NotNull
  val reductionStatus: String,

  @Column
  @NotNull
  val startDate: Date,

  @Column
  @NotNull
  val expiryDate: Date,

  @Column
  @NotNull
  val regionCode: String,

  @Column
  @NotNull
  val pduCode: String,

  @Column
  @NotNull
  val teamCode: String,

  @Column
  @NotNull
  val contractedHours: Int,

)
