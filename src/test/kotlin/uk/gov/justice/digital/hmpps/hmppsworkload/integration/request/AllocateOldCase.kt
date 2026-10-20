package uk.gov.justice.digital.hmpps.hmppsworkload.integration.request

fun allocateOldCase(crn: String, eventNumber: Int, sendEmailCopyToAllocatingOfficer: Boolean = true, allocationJustificationNotes: String = "some notes", sensitiveNotes: Boolean = false) = """
  {
     "crn": "$crn",
     "emailTo" : ["additionalEmailReceiver@test.justice.gov.uk"],
     "sendEmailCopyToAllocatingOfficer": $sendEmailCopyToAllocatingOfficer,
     "eventNumber": $eventNumber,
     "allocationJustificationNotes": "$allocationJustificationNotes",
     "sensitiveNotes": $sensitiveNotes,
     "laoCase": false
  }
""".trimIndent()
