package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.ColumnResult
import jakarta.persistence.ConstructorResult
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.NamedNativeQuery
import jakarta.persistence.SqlResultSetMapping
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.TeamOverview
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.WorkloadCaseResult
import java.math.BigInteger

@SqlResultSetMapping(
  name = "TeamOverviewResult",
  classes = [
    ConstructorResult(
      targetClass = TeamOverview::class,
      columns = [
        ColumnResult(name = "totalCommunityCases", type = Int::class),
        ColumnResult(name = "totalLicenseCases", type = Int::class),
        ColumnResult(name = "totalCustodyCases", type = Int::class),
        ColumnResult(name = "availablePoints", type = BigInteger::class),
        ColumnResult(name = "totalPoints", type = BigInteger::class),
        ColumnResult(name = "staffCode"),
        ColumnResult(name = "teamCode"),
      ],
    ),
  ],
)
@NamedNativeQuery(
  name = "TeamEntity.findAllByTeamCodes",
  resultSetMapping = "TeamOverviewResult",
  query = """SELECT
    w.total_filtered_community_cases as totalCommunityCases, w.total_filtered_license_cases as totalLicenseCases, w.total_filtered_custody_cases as totalCustodyCases , wpc.available_points AS availablePoints, wpc.total_points AS totalPoints, om."key" as staffCode, t.code as teamCode
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
    WHERE wr.effective_from IS NOT NULL AND wr.effective_to IS NULL AND t.code in ?1""",
)
@SqlResultSetMapping(
  name = "WorkloadCaseResult",
  classes = [
    ConstructorResult(
      targetClass = WorkloadCaseResult::class,
      columns = [
        ColumnResult(name = "total_cases", type = Int::class),
        ColumnResult(name = "available_points", type = Int::class),
        ColumnResult(name = "total_points", type = Int::class),
        ColumnResult(name = "team_code"),
      ],
    ),
  ],
)
@NamedNativeQuery(
  name = "TeamEntity.findWorkloadCountCaseByCode",
  resultSetMapping = "WorkloadCaseResult",
  query = """SELECT
    total_cases, available_points, total_points, t.code AS team_code
    FROM app.ldu_case_overview AS wo
    JOIN app.team AS t
        ON wo.link_id = t.id
    WHERE t.code IN ?1""",
)
@Entity
@Table(name = "team", schema = "app")
data class TeamEntity(
  @Id
  @Column
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column
  val code: String,

  @Column
  val description: String,

  @ManyToOne
  @JoinColumn(name = "ldu_id")
  val ldu: PduEntity,
)
