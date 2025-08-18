package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import java.math.BigInteger

@Suppress("LongMethod")
@Embeddable
data class CommunityTierPoints(
  @Column(name = "comm_tier_1")
  val A3Points: BigInteger,
  @Column(name = "comm_tier_2")
  val A2Points: BigInteger,
  @Column(name = "comm_tier_3")
  val A1Points: BigInteger,
  @Column(name = "comm_tier_4")
  val A0Points: BigInteger,
  @Column(name = "comm_tier_5")
  val B3Points: BigInteger,
  @Column(name = "comm_tier_6")
  val B2Points: BigInteger,
  @Column(name = "comm_tier_7")
  val B1Points: BigInteger,
  @Column(name = "comm_tier_8")
  val B0Points: BigInteger,
  @Column(name = "comm_tier_9")
  val C3Points: BigInteger,
  @Column(name = "comm_tier_10")
  val C2Points: BigInteger,
  @Column(name = "comm_tier_11")
  val C1Points: BigInteger,
  @Column(name = "comm_tier_12")
  val C0Points: BigInteger,
  @Column(name = "comm_tier_13")
  val D3Points: BigInteger,
  @Column(name = "comm_tier_14")
  val D2Points: BigInteger,
  @Column(name = "comm_tier_15")
  val D1Points: BigInteger,
  @Column(name = "comm_tier_16")
  val D0Points: BigInteger,
  @Column(name = "comm_tier_1_s")
  val A3SPoints: BigInteger,
  @Column(name = "comm_tier_2_s")
  val A2SPoints: BigInteger,
  @Column(name = "comm_tier_3_s")
  val A1SPoints: BigInteger,
  @Column(name = "comm_tier_4_s")
  val A0SPoints: BigInteger,
  @Column(name = "comm_tier_5_s")
  val B3SPoints: BigInteger,
  @Column(name = "comm_tier_6_s")
  val B2SPoints: BigInteger,
  @Column(name = "comm_tier_7_s")
  val B1SPoints: BigInteger,
  @Column(name = "comm_tier_8_s")
  val B0SPoints: BigInteger,
  @Column(name = "comm_tier_9_s")
  val C3SPoints: BigInteger,
  @Column(name = "comm_tier_10_s")
  val C2SPoints: BigInteger,
  @Column(name = "comm_tier_11_s")
  val C1SPoints: BigInteger,
  @Column(name = "comm_tier_12_s")
  val C0SPoints: BigInteger,
  @Column(name = "comm_tier_13_s")
  val D3SPoints: BigInteger,
  @Column(name = "comm_tier_14_s")
  val D2SPoints: BigInteger,
  @Column(name = "comm_tier_15_s")
  val D1SPoints: BigInteger,
  @Column(name = "comm_tier_16_s")
  val D0SPoints: BigInteger,
) {
  fun asMap(): Map<Tier, BigInteger> = mapOf(
    Tier.A3 to A3Points,
    Tier.A2 to A2Points,
    Tier.A1 to A1Points,
    Tier.A0 to A0Points,
    Tier.B3 to B3Points,
    Tier.B2 to B2Points,
    Tier.B1 to B1Points,
    Tier.B0 to B0Points,
    Tier.C3 to C3Points,
    Tier.C2 to C2Points,
    Tier.C1 to C1Points,
    Tier.C0 to C0Points,
    Tier.D3 to D3Points,
    Tier.D2 to D2Points,
    Tier.D1 to D1Points,
    Tier.D0 to D0Points,
    Tier.A3S to A3SPoints,
    Tier.A2S to A2SPoints,
    Tier.A1S to A1SPoints,
    Tier.A0S to A0SPoints,
    Tier.B3S to B3SPoints,
    Tier.B2S to B2SPoints,
    Tier.B1S to B1SPoints,
    Tier.B0S to B0SPoints,
    Tier.C3S to C3SPoints,
    Tier.C2S to C2SPoints,
    Tier.C1S to C1SPoints,
    Tier.C0S to C0SPoints,
    Tier.D3S to D3SPoints,
    Tier.D2S to D2SPoints,
    Tier.D1S to D1SPoints,
    Tier.D0S to D0SPoints,
  )
}
