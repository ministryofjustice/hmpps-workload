package uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses

fun staffByUserNameResponse(userName: String) = """
  {
    "email": "sheila.hancock@test.justice.gov.uk",
    "probationArea": {
      "code": "N01",
      "description": "NPS North West",
      "institution": {
        "code": "string",
        "description": "string",
        "establishmentType": {
          "code": "ABC123",
          "description": "Some description"
        },
        "institutionId": 0,
        "institutionName": "string",
        "isEstablishment": true,
        "isPrivate": true,
        "nomsPrisonInstitutionCode": "string"
      },
      "nps": true,
      "organisation": {
        "code": "ABC123",
        "description": "Some description"
      },
      "probationAreaId": 0,
      "teams": [
        {
          "borough": {
            "code": "ABC123",
            "description": "Some description"
          },
          "code": "T1",
          "description": "Test Team",
          "district": {
            "code": "ABC123",
            "description": "Some description"
          },
          "externalProvider": {
            "code": "ABC123",
            "description": "Some description"
          },
          "isPrivate": true,
          "localDeliveryUnit": {
            "code": "ABC123",
            "description": "Some description"
          },
          "name": "string",
          "providerTeamId": 0,
          "scProvider": {
            "code": "ABC123",
            "description": "Some description"
          },
          "teamId": 0
        }
      ]
    },
    "staff": {
      "forenames": "Ben",
      "surname": "Doe"
    },
    "staffCode": "STAFF1",
    "staffGrade": {
      "code": "PSM",
      "description": "Some description"
    },
    "staffIdentifier": 123456,
    "teams": [
      {
        "borough": {
          "code": "ABC123",
          "description": "Some description"
        },
        "code": "T1",
        "description": "Test Team",
        "district": {
          "code": "ABC123",
          "description": "Some description"
        },
        "emailAddress": "first.last@digital.justice.gov.uk",
        "endDate": "2022-04-04",
        "localDeliveryUnit": {
          "code": "ABC123",
          "description": "Some description"
        },
        "startDate": "2022-04-04",
        "teamType": {
          "code": "ABC123",
          "description": "Some description"
        },
        "telephone": "OMU A"
      }
    ],
    "telephoneNumber": "020 1111 2222",
    "username": "$userName"
  }
""".trimIndent()
