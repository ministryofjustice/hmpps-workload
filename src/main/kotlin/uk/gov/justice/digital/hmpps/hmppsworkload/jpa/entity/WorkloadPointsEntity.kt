package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.math.BigInteger
import java.time.ZonedDateTime

@Entity
@Table(name = "workload_points", schema = "app")
data class WorkloadPointsEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "effective_from")
  val effectiveFrom: ZonedDateTime,

  @Column(name = "effective_to")
  val effectiveTo: ZonedDateTime?,

  @Column(name = "is_t2a")
  val isT2A: Boolean,

  @Column(name = "default_contracted_hours_po")
  val defaultContractedHoursPO: BigDecimal,

  @Column(name = "default_contracted_hours_spo")
  val defaultContractedHoursSPO: BigDecimal,

  @Column(name = "default_contracted_hours_pso")
  val defaultContractedHoursPSO: BigDecimal,

  @Column(name = "nominal_target_spo")
  val defaultAvailablePointsSPO: BigDecimal,

  @Column(name = "nominal_target_po")
  val defaultAvailablePointsPO: BigDecimal,

  @Column(name = "sdr")
  val standardCourtReportPoints: BigInteger,

  @Column(name = "sdr_conversion")
  val fastCourtReportPoints: BigInteger,

  @Column(name = "parom")
  val paroleReportWeighting: Int,

  @Column(name = "paroms_enabled")
  val paroleReportWeightingEnabled: Boolean,

  @Column(name = "weighting_arms_lic")
  val licenseARMAssessmentWeighting: BigInteger,

  @Column(name = "weighting_arms_comm")
  val communityARMAssessmentWeighting: BigInteger,

) {
  fun getDefaultPointsAvailable(grade: String): BigDecimal = when (grade) {
    "SPO" -> defaultAvailablePointsSPO
    else -> defaultAvailablePointsPO
  }
  fun getDefaultContractedHours(grade: String): BigDecimal = when (grade) {
    "PO", "PQiP" -> defaultContractedHoursPO
    "PSO" -> defaultContractedHoursPSO
    else -> defaultContractedHoursSPO
  }
}
