package uk.gov.justice.digital.hmpps.hmppsworkload.jpa.entity

import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.TeamOverview
import javax.persistence.Column
import javax.persistence.ColumnResult
import javax.persistence.ConstructorResult
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.NamedNativeQuery
import javax.persistence.SqlResultSetMapping
import javax.persistence.Table

@SqlResultSetMapping(
  name = "TeamOverviewResult",
  classes = [
    ConstructorResult(
      targetClass = TeamOverview::class,
      columns = [
        ColumnResult(name = "forename"),
        ColumnResult(name = "surname"),
        ColumnResult(name = "total_community_cases"),
        ColumnResult(name = "total_filtered_custody_cases"),
        ColumnResult(name = "available_points"),
        ColumnResult(name = "total_points"),
        ColumnResult(name = "key")
      ]
    )
  ]
)
@NamedNativeQuery(
  name = "TeamEntity.findByOverview",
  resultSetMapping = "TeamOverviewResult",
  query = """SELECT
    om.forename,om.surname, (w.total_filtered_community_cases + w.total_filtered_license_cases) as total_community_cases, w.total_filtered_custody_cases , wpc.available_points AS available_points, wpc.total_points AS total_points, om."key"
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
    WHERE wr.effective_from IS NOT NULL AND wr.effective_to IS NULL AND t.code = ?1"""
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
  val ldu: LduEntity,
)
