package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "court_reports", schema = "staging")
data class WMTCourtReportsEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column
  val teamCode: String,

  @Column(name = "om_key")
  val staffCode: String,

  @Column(name = "sdr_last_30")
  val standardDeliveryReportCount: Int? = null,

  @Column(name = "sdr_conv_last_30")
  val fastDeliveryReportCount: Int? = null,
)
