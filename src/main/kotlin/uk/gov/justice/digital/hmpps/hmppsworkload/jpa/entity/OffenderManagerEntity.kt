package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.NamedNativeQuery
import jakarta.persistence.Table

@NamedNativeQuery(
  name = "OffenderManagerEntity.findCasesByTeamCodeAndStaffCode",
  query = """
  SELECT
   c.case_ref_no
    FROM app.case_details AS c
    JOIN app.workload AS w
        ON c.workload_id = w.id
    JOIN app.workload_owner AS wo
        ON w.workload_owner_id = wo.id
    JOIN app.offender_manager AS om
        ON wo.offender_manager_id = om.id
    JOIN app.team AS t
        ON wo.team_id = t.id
    JOIN app.workload_report AS wr
        ON w.workload_report_id = wr.id
    WHERE wr.effective_from IS NOT NULL AND wr.effective_to IS null and c.row_type = 'N' and t.code = ?2 and om."key" = ?1
""",
)
@NamedNativeQuery(
  name = "OffenderManagerEntity.findCaseByTeamCodeAndStaffCodeAndCrn",
  query = """
  SELECT
   c.case_ref_no
    FROM app.case_details AS c
    JOIN app.workload AS w
        ON c.workload_id = w.id
    JOIN app.workload_owner AS wo
        ON w.workload_owner_id = wo.id
    JOIN app.offender_manager AS om
        ON wo.offender_manager_id = om.id
    JOIN app.team AS t
        ON wo.team_id = t.id
    JOIN app.workload_report AS wr
        ON w.workload_report_id = wr.id
    WHERE wr.effective_from IS NOT NULL AND wr.effective_to IS null and c.row_type = 'N' and t.code = ?1 and om."key" = ?2 and c.case_ref_no = ?3
""",
)
@Entity
@Table(name = "offender_manager", schema = "app")
data class OffenderManagerEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "key")
  val code: String,

  @Column
  val forename: String,

  @Column
  val surname: String,

  @Column
  val typeId: Long,
)
