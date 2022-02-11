package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import java.math.BigInteger
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "workload_points", schema = "app")
data class WorkloadPointsEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Embedded
  val communityTierPoints: CommunityTierPoints,

  @Embedded
  val licenseTierPoints: LicenseTierPoints,

  @Embedded
  val custodyTierPoints: CustodyTierPoints,

  @Column(name = "effective_from")
  val effectiveFrom: ZonedDateTime,

  @Column(name = "effective_to")
  val effectiveTo: ZonedDateTime,

  @Column(name = "is_t2a")
  val isT2A: Boolean

) {
  fun getTierPointsMap(caseType: CaseType): Map<Tier, BigInteger> = when (caseType) {
    CaseType.CUSTODY -> custodyTierPoints.asMap()
    CaseType.LICENSE -> licenseTierPoints.asMap()
    CaseType.COMMUNITY -> communityTierPoints.asMap()
  }
}