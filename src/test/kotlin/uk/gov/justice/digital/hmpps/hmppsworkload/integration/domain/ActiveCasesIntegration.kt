package uk.gov.justice.digital.hmpps.hmppsworkload.integration.domain

data class ActiveCasesIntegration(
  val crn: String,
  val firstName: String,
  val surname: String,
  val type: String,
)

data class StaffActiveCasesIntegration(
  val staffCode: String,
  val staffGrade: String = "PO",
  val email: String? = "sheila.hancock@test.justice.gov.uk",
  val activeCases: List<ActiveCasesIntegration> = emptyList(),
)
