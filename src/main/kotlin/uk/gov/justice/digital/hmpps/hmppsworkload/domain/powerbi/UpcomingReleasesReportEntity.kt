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
data class UpcomingReleasesReportEntity @JsonCreator constructor(

  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column
  @NotNull
  val expectedReleaseDate: Date,

  @Column
  @NotNull
  val team: String,

  @Column
  @NotNull
  val probationPractitioner: String,
)
