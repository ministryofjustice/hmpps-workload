package uk.gov.justice.digital.hmpps.hmppsworkload.integration.request

fun allocateCase(crn: String, eventNumber: Int, sendEmailCopyToAllocatingOfficer: Boolean = true, spoOversightNotes: String = "Overs notes", sensitiveOversightNotes: Boolean = false) = """
  {
     "crn": "$crn",
     "emailTo" : ["additionalEmailReceiver@test.justice.gov.uk"],
     "sendEmailCopyToAllocatingOfficer": $sendEmailCopyToAllocatingOfficer,
     "eventNumber": $eventNumber,
     "spoOversightNotes": "$spoOversightNotes",
     "sensitiveOversightNotes": $sensitiveOversightNotes,
     "laoCase": false
  }
""".trimIndent()
