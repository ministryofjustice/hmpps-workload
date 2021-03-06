package uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses

fun oneOffenderSearchByCrnsResponse() = """
  [
    {
      "accessDenied": true,
      "activeProbationManagedSentence": true,
      "age": 0,
      "contactDetails": {
        "addresses": [
          {
            "addressNumber": "string",
            "buildingName": "string",
            "county": "string",
            "district": "string",
            "from": "2022-04-07",
            "noFixedAbode": true,
            "notes": "string",
            "postcode": "string",
            "status": {
              "code": "string",
              "description": "string"
            },
            "streetName": "string",
            "telephoneNumber": "string",
            "to": "2022-04-07",
            "town": "string"
          }
        ],
        "allowSMS": true,
        "emailAddresses": [
          "string"
        ],
        "phoneNumbers": [
          {
            "number": "string",
            "type": "MOBILE"
          }
        ]
      },
      "currentDisposal": "string",
      "currentExclusion": true,
      "currentRestriction": true,
      "currentTier": "string",
      "dateOfBirth": "2022-04-07",
      "exclusionMessage": "string",
      "firstName": "John",
      "gender": "string",
      "highlight": "{\"surname\": [\"Smith\"], \"offenderAliases.surname\": [\"SMITH\"]}",
      "mappa": {
        "category": 0,
        "categoryDescription": "string",
        "level": 0,
        "levelDescription": "string",
        "notes": "string",
        "officer": {
          "code": "AN001A",
          "forenames": "Sheila Linda",
          "surname": "Hancock",
          "unallocated": false
        },
        "probationArea": {
          "code": "string",
          "description": "string"
        },
        "reviewDate": "2022-04-07",
        "startDate": "2022-04-07",
        "team": {
          "code": "string",
          "description": "string"
        }
      },
      "middleNames": [
        "William"
      ],
      "offenderAliases": [
        {
          "dateOfBirth": "2022-04-07",
          "firstName": "string",
          "gender": "string",
          "id": "string",
          "middleNames": [
            "string"
          ],
          "surname": "string"
        }
      ],
      "offenderId": 0,
      "offenderManagers": [
        {
          "active": true,
          "allocationReason": {
            "code": "string",
            "description": "string"
          },
          "fromDate": "2022-04-07",
          "partitionArea": "string",
          "probationArea": {
            "code": "string",
            "description": "string",
            "institution": {
              "code": "string",
              "description": "string",
              "establishmentType": {
                "code": "string",
                "description": "string"
              },
              "institutionId": 0,
              "institutionName": "string",
              "isEstablishment": true,
              "isPrivate": true,
              "nomsPrisonInstitutionCode": "string"
            },
            "nps": true,
            "organisation": {
              "code": "string",
              "description": "string"
            },
            "probationAreaId": 0,
            "teams": [
              {
                "borough": {
                  "code": "string",
                  "description": "string"
                },
                "code": "string",
                "description": "string",
                "district": {
                  "code": "string",
                  "description": "string"
                },
                "externalProvider": {
                  "code": "string",
                  "description": "string"
                },
                "isPrivate": true,
                "localDeliveryUnit": {
                  "code": "string",
                  "description": "string"
                },
                "name": "string",
                "providerTeamId": 0,
                "scProvider": {
                  "code": "string",
                  "description": "string"
                },
                "teamId": 0
              }
            ]
          },
          "providerEmployee": {
            "forenames": "Sheila Linda",
            "surname": "Hancock"
          },
          "softDeleted": true,
          "staff": {
            "code": "AN001A",
            "forenames": "Sheila Linda",
            "surname": "Hancock",
            "unallocated": false
          },
          "team": {
            "borough": {
              "code": "string",
              "description": "string"
            },
            "code": "C01T04",
            "description": "OMU A",
            "district": {
              "code": "string",
              "description": "string"
            },
            "localDeliveryUnit": {
              "code": "string",
              "description": "string"
            },
            "telephone": "OMU A"
          },
          "toDate": "2022-04-07",
          "trustOfficer": {
            "forenames": "Sheila Linda",
            "surname": "Hancock"
          }
        }
      ],
      "offenderProfile": {
        "disabilities": [
          {
            "disabilityId": 0,
            "disabilityType": {
              "code": "string",
              "description": "string"
            },
            "endDate": "2022-04-07",
            "notes": "string",
            "startDate": "2022-04-07"
          }
        ],
        "ethnicity": "string",
        "immigrationStatus": "string",
        "nationality": "string",
        "notes": "string",
        "offenderDetails": "string",
        "offenderLanguages": {
          "languageConcerns": "string",
          "otherLanguages": [
            "string"
          ],
          "primaryLanguage": "string",
          "requiresInterpreter": true
        },
        "previousConviction": {
          "convictionDate": "2022-04-07",
          "detail": {
            "additionalProp1": "string",
            "additionalProp2": "string",
            "additionalProp3": "string"
          }
        },
        "religion": "string",
        "remandStatus": "string",
        "riskColour": "string",
        "secondaryNationality": "string",
        "sexualOrientation": "string"
      },
      "otherIds": {
        "crn": "CRN1111",
        "croNumber": "string",
        "immigrationNumber": "string",
        "mostRecentPrisonerNumber": "string",
        "niNumber": "string",
        "nomsNumber": "string",
        "pncNumber": "string"
      },
      "partitionArea": "string",
      "previousSurname": "string",
      "probationStatus": {
        "awaitingPsr": true,
        "inBreach": true,
        "preSentenceActivity": true,
        "previouslyKnownTerminationDate": "2022-04-07",
        "status": "string"
      },
      "restrictionMessage": "string",
      "softDeleted": false,
      "surname": "Doe",
      "title": "string"
    }
  ]
""".trimIndent()
