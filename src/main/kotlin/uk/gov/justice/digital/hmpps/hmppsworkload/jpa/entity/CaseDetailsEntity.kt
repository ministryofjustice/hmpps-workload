package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.converter.TierConverter

@Entity
@Table(name = "CASE_DETAILS")
data class CaseDetailsEntity(

  @Id
  @Column
  val crn: String,

  @Column
  @Convert(converter = TierConverter::class)
  var tier: Tier,

  @Column
  var provisionalTier: Boolean,

  @Column
  @Enumerated(EnumType.STRING)
  var type: CaseType,

  @Column
  var firstName: String,

  @Column
  var surname: String,

)
