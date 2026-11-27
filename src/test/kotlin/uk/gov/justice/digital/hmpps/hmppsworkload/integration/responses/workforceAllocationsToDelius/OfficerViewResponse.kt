package uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses.workforceAllocationsToDelius

fun officerOverviewResponse(staffCode: String, staffGrade: String, email: String?): String {
  val emailEntry = email?.let { "\"email\": \"$email\"," } ?: ""
  return """
{
  "code": "$staffCode",
  "name": {
    "forename": "Sheila",
    "middleName": "MiddleName",
    "surname": "Hancock"
  },
  "grade": "$staffGrade",
  $emailEntry
  "casesDueToEndInNext4Weeks": 1,
  "releasesWithinNext4Weeks": 1,
  "paroleReportsToCompleteInNext4Weeks": 1
}
  """.trimIndent()
}
