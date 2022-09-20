package uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses

fun offenderSummaryResponse() = """
  {
      "offenderId": 9999999999,
      "firstName": "Jane",
      "middleNames": [
          "hi",
          "hi"
      ],
      "surname": "Doe",
      "dateOfBirth": "2001-11-17",
      "gender": "Male",
      "otherIds": {
          "crn": "XXXXXXX",
          "pncNumber": "9999/1234567A"
      },
      "contactDetails": {},
      "offenderProfile": {
          "offenderLanguages": {},
          "previousConviction": {}
      },
      "softDeleted": false,
      "currentDisposal": "1",
      "partitionArea": "National Data",
      "currentRestriction": false,
      "currentExclusion": false,
      "activeProbationManagedSentence": true
  }
""".trimIndent()
