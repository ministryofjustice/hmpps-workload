package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.CaseType
import uk.gov.justice.digital.hmpps.hmppsworkload.domain.Tier

@Entity
@Table(name = "CASE_DETAILS")
data class CaseDetailsEntity(

  @Id
  @Column
  val crn: String,

  @Column
  @Enumerated(EnumType.STRING)
  var tier: Tier,

  @Column
  @Enumerated(EnumType.STRING)
  var type: CaseType,

  @Column
  var firstName: String,

  @Column
  var surname: String,

)
