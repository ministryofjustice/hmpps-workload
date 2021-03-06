package uk.gov.justice.digital.hmpps.hmppsworkload.integration.responses

fun teamStaffResponse() = """
  [
      {
          "staffCode": "OM1",
          "staffIdentifier": 123456789,
          "staff": {
              "forenames": "Ben",
              "surname": "Doe"
          },
          "staffGrade": {
            "code": "PSM",
            "description": "Some description"
          }
      },
      {
          "staffCode": "OM2",
          "staffIdentifier": 234567891,
          "staff": {
              "forenames": "Sally",
              "surname": "Socks"
          },
          "staffGrade": {
            "code": "PSM",
            "description": "Some description"
          }
      },
      {
          "staffCode": "NOWORKLOAD1",
          "staffIdentifier": 987654321,
          "staff": {
              "forenames": "Jane",
              "surname": "Doe"
          }
      },
      {
          "staffCode": "OM3",
          "staffIdentifier": 234567891,
          "staff": {
              "forenames": "Billy",
              "surname": "Smith"
          },
          "staffGrade": {
            "code": "PSP",
            "description": "Some description"
          }
          
      }
  ]
""".trimIndent()
