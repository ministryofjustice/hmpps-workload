package uk.gov.justice.digital.hmpps.hmppsworkload.client

class ClientResponses {

  companion object {

    fun deliusResponseGetAllowedTeamInfo(): String = """
      {
        "teams": [
          {
            "code": "TM1",
            "description": "Team1",
            "localAdminUnit": {
              "code": "LAU1",
              "description": "Lau1",
              "probationDeliveryUnit": {
                "code": "PDU1",
                "description": "Pdu1",
                "provider": {
                  "code": "REG1",
                  "description": "Region1"
                }
              }
            }
          }
        ]
      }
    """.trimIndent()

    fun deliusResponsePostAllocationDetails(): String = """
      {
        "cases": [
          {
            "crn": "X999999",
            "name": {
              "forename": "Bob",
              "middleName": "Tim",
              "surname": "Jones"
            },
            "staff": {
              "code": "SM00234",
              "name": {
                "forename": "Paula",
                "middleName": "",
                "surname": "Fish"
              },
              "email": "example@example.com",
              "grade": "PO"
            }
          }
        ]
      }
    """.trimIndent()

    fun deliusResponseAllocationDetails(): String = """
      {
        "crn": "X999999",
        "name": {
          "forename": "BOB",
          "middleName": "TOM",
          "surname": "SMITH"
        },
        "event": {
          "number": "2",
          "manager": {
            "code": "SM00235",
            "name": {
              "forename": "PAUL",
              "middleName": "",
              "surname": "JONES"
            },
            "teamCode": "TM1",
            "grade": "SPO",
            "allocated": true
          }
        },
        "type": "LICENSE",
        "initialAppointment": {
          "date": "2025-05-02",
          "staff": {
            "code": "SM00234",
            "name": {
              "forename": "PAULA",
              "middleName": "",
              "surname": "FISH"
            },
            "email": "example@example.com",
            "grade": "PO"
          }
        },
        "staff": {
          "code": "SM00234",
          "name": {
            "forename": "PAULA",
            "middleName": "",
            "surname": "FISH"
          },
          "email": "example@example.com",
          "grade": "PO"
        }
      }
    """.trimIndent()

    fun deliusResponseStaffActiveCases(): String = """
      {
        "code": "SM00234",
        "name": {
          "forename": "David",
          "middleName": "Paul",
          "surname": "Jones"
        },
        "grade": "PSO",
        "email": "example@example.com",
        "cases": [
          {
            "crn": "X999999",
            "name": {
              "forename": "Bob",
              "middleName": "Timothy",
              "surname": "Smith"
            },
            "type": "LICENSE"
          }
        ]
      }
    """.trimIndent()

    fun deliusResponseAllocationCompleteDetails(): String = """
      {
        "crn": "X999999",
        "name": {
          "forename": "John",
          "middleName": "Paul",
          "surname": "Jones"
        },
        "event": {
          "number": "1",
          "manager": {
            "code": "SM00235",
            "name": {
              "forename": "Jenny",
              "middleName": "Sharon",
              "surname": "Smith"
            },
            "teamCode": "TM1",
            "grade": "SPO",
            "allocated": true
          }
        },
        "type": "LICENSE",
        "initialAppointment": {
          "date": "2025-05-22",
          "staff": {
            "code": "SM00234",
            "name": {
              "forename": "Peter",
              "middleName": "David",
              "surname": "Jones"
            },
            "email": "example@example.com",
            "grade": "PO"
          }
        },
        "staff": {
          "code": "SM00234",
          "name": {
            "forename": "Peter",
            "middleName": "David",
            "surname": "Jones"
          },
          "email": "example@example.com",
          "grade": "PO"
        }
      }
    """.trimIndent()

    fun deliusResponseGetImpact(): String = """
      {
        "crn": "X999999",
        "name": {
          "forename": "Paul",
          "middleName": "John",
          "surname": "Smith"
        },
        "staff": {
          "code": "SM00234",
          "name": {
            "forename": "Peter",
            "middleName": "David",
            "surname": "Jones"
          },
          "email": "example@example.com",
          "grade": "PSO"
        }
      }
    """.trimIndent()

    fun deliusResponseGetOfficerView(): String = """
      {
        "code": "SM00234",
        "name": {
          "forename": "John",
          "middleName": "Paul",
          "surname": "Smith"
        },
        "grade": "PSO",
        "email": "example@example.com",
        "casesDueToEndInNext4Weeks": 20,
        "releasesWithinNext4Weeks": 15,
        "paroleReportsToCompleteInNext4Weeks": 7
      }
    """.trimIndent()

    fun deliusResponseGetPersonByCRN(): String = """
      {
        "crn": "X999999",
        "name": {
          "forename": "John",
          "middleName": "A",
          "surname": "Doe"
        },
        "type": "LICENSE"
      }
    """.trimIndent()

    fun deliusResponseChoosePractitionersNoCRN(): String = """
      {
        "teams": {
          "teamA": [
            {
              "code": "SM001",
              "name": {
                "forename": "Bob",
                "middleName": null,
                "surname": "Brown"
              },
              "email": "bob.brown@example.com",
              "grade": "Officer"
            }
          ],
          "teamB": [
            {
              "code": "SM002",
              "name": {
                "forename": "Carol",
                "middleName": "D",
                "surname": "Jones"
              },
              "email": null,
              "grade": "Supervisor"
            },
            {
              "code": "SM003",
              "name": {
                "forename": "Dave",
                "middleName": "",
                "surname": "Williams"
              },
              "email": "dave.williams@example.com",
              "grade": null
            }
          ]
        }
      }
    """.trimIndent()

    fun deliusResponseChoosePractitioners(): String = """
      {
        "crn": "X999999",
        "name": {
          "forename": "John",
          "middleName": "A",
          "surname": "Doe"
        },
        "probationStatus": {
          "status": "CURRENTLY_MANAGED",
          "description": "Currently under supervision"
        },
        "communityPersonManager": {
          "code": "CPM001",
          "name": {
            "forename": "Alice",
            "middleName": "B",
            "surname": "Smith"
          },
          "grade": "Senior Officer",
          "teamCode": "TEAM42"
        },
        "teams": {
          "teamA": [
            {
              "code": "SM001",
              "name": {
                "forename": "Bob",
                "middleName": null,
                "surname": "Brown"
              },
              "email": "bob.brown@example.com",
              "grade": "Officer"
            }
          ],
          "teamB": [
            {
              "code": "SM002",
              "name": {
                "forename": "Carol",
                "middleName": "D",
                "surname": "Jones"
              },
              "email": null,
              "grade": "Supervisor"
            },
            {
              "code": "SM003",
              "name": {
                "forename": "Dave",
                "middleName": "",
                "surname": "Williams"
              },
              "email": "dave.williams@example.com",
              "grade": null
            }
          ]
        }
      }
    """.trimIndent()
  }
}
