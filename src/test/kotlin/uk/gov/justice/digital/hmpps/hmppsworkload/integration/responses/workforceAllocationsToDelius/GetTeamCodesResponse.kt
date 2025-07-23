package uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses.workforceAllocationsToDelius

fun getTeamCodesResponse() = """
{
  "datasets": [
    {
      "code": "DS1",
      "description": "Data set descriptio"
    }
  ],
  "teams": [
    {
      "code": "team1",
      "description": "team 1 description",
      "localAdminUnit": {
        "code": "LAU1",
        "description": "Landan town",
        "probationDeliveryUnit": {
          "code": "Post",
          "description": "hahaha",
          "provider": {
            "code": "Post office",
            "description": "Mail"
          }
        }
      }
    }
  ]
}
""".trimIndent()
