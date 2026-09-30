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
import uk.gov.justice.digital.hmpps.hmppsworkload.jpa.mapping.WorkloadCaseResult

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
