package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.ColumnResult
import jakarta.persistence.ConstructorResult
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.NamedNativeQuery
import jakarta.persistence.SqlResultSetMapping
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.OverviewOffenderManager
import java.math.BigInteger
import java.time.LocalDateTime

@SqlResultSetMapping(
  name = "OffenderManagerOverviewResult",
  classes = [
    ConstructorResult(
      targetClass = OverviewOffenderManager::class,
      columns = [
        ColumnResult(name = "totalCommunityCases", type = Long::class),
        ColumnResult(name = "totalLicenseCases", type = Long::class),
        ColumnResult(name = "totalCustodyCases", type = Long::class),
        ColumnResult(name = "availablePoints", type = BigInteger::class),
        ColumnResult(name = "totalPoints", type = BigInteger::class),
        ColumnResult(name = "code"),
        ColumnResult(name = "lastUpdatedOn", type = LocalDateTime::class),
        ColumnResult(name = "workloadOwnerId", type = Long::class),
        ColumnResult(name = "paroleReportsDue", type = BigInteger::class),
      ],
    ),
  ],
)
@NamedNativeQuery(
  name = "OffenderManagerEntity.findByOverview",
  resultSetMapping = "OffenderManagerOverviewResult",
  query = """SELECT
    w.total_filtered_community_cases  as totalCommunityCases,  w.total_filtered_license_cases as totalLicenseCases, w.total_filtered_custody_cases as totalCustodyCases , wpc.available_points AS availablePoints, wpc.total_points AS totalPoints, om."key" as code, wpc.last_updated_on as lastUpdatedOn, wo.id as workloadOwnerId, w.paroms_due_next_30_days as paroleReportsDue
    FROM app.workload_owner AS wo
    JOIN app.team AS t
        ON wo.team_id = t.id
    JOIN app.workload AS w
        ON wo.id = w.workload_owner_id
    JOIN app.workload_points_calculations AS wpc
        ON wpc.workload_id = w.id
    JOIN app.workload_report AS wr
        ON wr.id = wpc.workload_report_id
    JOIN app.offender_manager AS om
        ON om.id = wo.offender_manager_id
    WHERE wr.effective_from IS NOT NULL AND wr.effective_to IS NULL AND t.code = ?1 AND om."key" = ?2""",
)
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
