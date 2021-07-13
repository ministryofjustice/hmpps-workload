
ALTER VIEW [app].[national_capacity_view]
WITH SCHEMABINDING
    AS
    SELECT SUM(total_points) AS total_points
      , SUM(available_points) AS available_points
      , SUM(reduction_hours) AS reduction_hours
      , SUM(wpc.contracted_hours) AS contracted_hours
      , wr.effective_from AS effective_from
      , wr.id AS workload_report_id
      , COUNT_BIG(*) AS count
    FROM app.workload_points_calculations AS wpc
      JOIN app.workload AS w ON wpc.workload_id = w.id
      JOIN app.workload_report AS wr ON wpc.workload_report_id = wr.id
      JOIN app.workload_owner AS wo ON wo.id = w.workload_owner_id
            JOIN app.team AS t ON t.id = wo.team_id
            JOIN app.ldu AS l ON l.id = t.ldu_id
            JOIN app.region AS r ON r.id = l.region_id
          WHERE r.description LIKE 'NPS%'
    GROUP BY wr.effective_from, wr.id;
GO

ALTER VIEW [app].[national_caseload_view]
        WITH SCHEMABINDING
        AS
  SELECT
      r.id AS link_id
    , r.description AS name
    , r.description AS region_name
    , omt.grade_code
    , tr.location
    , SUM((CASE WHEN tr.tier_number = 0 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 0 THEN tr.t2a_total_cases ELSE 0 END)) AS untiered
    , SUM((CASE WHEN tr.tier_number = 1 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 1 THEN tr.t2a_total_cases ELSE 0 END)) AS a3
    , SUM((CASE WHEN tr.tier_number = 2 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 2 THEN tr.t2a_total_cases ELSE 0 END)) AS a2
    , SUM((CASE WHEN tr.tier_number = 3 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 3 THEN tr.t2a_total_cases ELSE 0 END)) AS a1
    , SUM((CASE WHEN tr.tier_number = 4 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 4 THEN tr.t2a_total_cases ELSE 0 END)) AS a0
    , SUM((CASE WHEN tr.tier_number = 5 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 5 THEN tr.t2a_total_cases ELSE 0 END)) AS b3
    , SUM((CASE WHEN tr.tier_number = 6 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 6 THEN tr.t2a_total_cases ELSE 0 END)) AS b2
    , SUM((CASE WHEN tr.tier_number = 7 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 7 THEN tr.t2a_total_cases ELSE 0 END)) AS b1
    , SUM((CASE WHEN tr.tier_number = 8 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 8 THEN tr.t2a_total_cases ELSE 0 END)) AS b0
    , SUM((CASE WHEN tr.tier_number = 9 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 9 THEN tr.t2a_total_cases ELSE 0 END)) AS c3
    , SUM((CASE WHEN tr.tier_number = 10 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 10 THEN tr.t2a_total_cases ELSE 0 END)) AS c2
    , SUM((CASE WHEN tr.tier_number = 11 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 11 THEN tr.t2a_total_cases ELSE 0 END)) AS c1
    , SUM((CASE WHEN tr.tier_number = 12 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 12 THEN tr.t2a_total_cases ELSE 0 END)) AS c0
    , SUM((CASE WHEN tr.tier_number = 13 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 13 THEN tr.t2a_total_cases ELSE 0 END)) AS d3
    , SUM((CASE WHEN tr.tier_number = 14 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 14 THEN tr.t2a_total_cases ELSE 0 END)) AS d2
    , SUM((CASE WHEN tr.tier_number = 15 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 15 THEN tr.t2a_total_cases ELSE 0 END)) AS d1
    , SUM((CASE WHEN tr.tier_number = 16 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 16 THEN tr.t2a_total_cases ELSE 0 END)) AS d0
    , SUM(tr.total_filtered_cases + tr.t2a_total_cases) AS total_cases
    , COUNT_BIG(*) AS count
  FROM app.tiers tr
      JOIN app.workload w ON tr.workload_id = w.id
      JOIN app.workload_points_calculations wpc ON wpc.workload_id = w.id
      JOIN app.workload_report wr ON wr.id = wpc.workload_report_id
      JOIN app.workload_owner wo ON wo.id = w.workload_owner_id
      JOIN app.team t ON t.id = wo.team_id
      JOIN app.ldu l ON l.id = t.ldu_id
      JOIN app.region r ON r.id = l.region_id
      JOIN app.offender_manager om ON om.id = wo.offender_manager_id
      JOIN app.offender_manager_type omt ON omt.id = om.type_id
  WHERE wr.effective_from IS NOT NULL
      AND wr.effective_to IS NULL AND r.description LIKE 'NPS%'
  GROUP BY r.id, r.description, omt.grade_code, tr.location;
GO

GO

CREATE VIEW [app].[crc_caseload_view]
        WITH SCHEMABINDING
        AS
  SELECT
      r.id AS link_id
    , r.description AS name
    , r.description AS region_name
    , omt.grade_code
    , tr.location
    , SUM((CASE WHEN tr.tier_number = 0 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 0 THEN tr.t2a_total_cases ELSE 0 END)) AS untiered
    , SUM((CASE WHEN tr.tier_number = 1 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 1 THEN tr.t2a_total_cases ELSE 0 END)) AS a3
    , SUM((CASE WHEN tr.tier_number = 2 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 2 THEN tr.t2a_total_cases ELSE 0 END)) AS a2
    , SUM((CASE WHEN tr.tier_number = 3 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 3 THEN tr.t2a_total_cases ELSE 0 END)) AS a1
    , SUM((CASE WHEN tr.tier_number = 4 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 4 THEN tr.t2a_total_cases ELSE 0 END)) AS a0
    , SUM((CASE WHEN tr.tier_number = 5 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 5 THEN tr.t2a_total_cases ELSE 0 END)) AS b3
    , SUM((CASE WHEN tr.tier_number = 6 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 6 THEN tr.t2a_total_cases ELSE 0 END)) AS b2
    , SUM((CASE WHEN tr.tier_number = 7 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 7 THEN tr.t2a_total_cases ELSE 0 END)) AS b1
    , SUM((CASE WHEN tr.tier_number = 8 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 8 THEN tr.t2a_total_cases ELSE 0 END)) AS b0
    , SUM((CASE WHEN tr.tier_number = 9 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 9 THEN tr.t2a_total_cases ELSE 0 END)) AS c3
    , SUM((CASE WHEN tr.tier_number = 10 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 10 THEN tr.t2a_total_cases ELSE 0 END)) AS c2
    , SUM((CASE WHEN tr.tier_number = 11 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 11 THEN tr.t2a_total_cases ELSE 0 END)) AS c1
    , SUM((CASE WHEN tr.tier_number = 12 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 12 THEN tr.t2a_total_cases ELSE 0 END)) AS c0
    , SUM((CASE WHEN tr.tier_number = 13 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 13 THEN tr.t2a_total_cases ELSE 0 END)) AS d3
    , SUM((CASE WHEN tr.tier_number = 14 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 14 THEN tr.t2a_total_cases ELSE 0 END)) AS d2
    , SUM((CASE WHEN tr.tier_number = 15 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 15 THEN tr.t2a_total_cases ELSE 0 END)) AS d1
    , SUM((CASE WHEN tr.tier_number = 16 THEN tr.total_filtered_cases ELSE 0 END) + (CASE WHEN tr.tier_number = 16 THEN tr.t2a_total_cases ELSE 0 END)) AS d0
    , SUM(tr.total_filtered_cases + tr.t2a_total_cases) AS total_cases
    , COUNT_BIG(*) AS count
  FROM app.tiers tr
      JOIN app.workload w ON tr.workload_id = w.id
      JOIN app.workload_points_calculations wpc ON wpc.workload_id = w.id
      JOIN app.workload_report wr ON wr.id = wpc.workload_report_id
      JOIN app.workload_owner wo ON wo.id = w.workload_owner_id
      JOIN app.team t ON t.id = wo.team_id
      JOIN app.ldu l ON l.id = t.ldu_id
      JOIN app.region r ON r.id = l.region_id
      JOIN app.offender_manager om ON om.id = wo.offender_manager_id
      JOIN app.offender_manager_type omt ON omt.id = om.type_id
  WHERE wr.effective_from IS NOT NULL
      AND wr.effective_to IS NULL AND r.description NOT LIKE 'NPS%'
  GROUP BY r.id, r.description, omt.grade_code, tr.location;

GO

CREATE UNIQUE CLUSTERED INDEX idx_crc_caseload_view ON app.crc_caseload_view (link_id, location, grade_code)

GO

CREATE VIEW app.crc_capacity_view
  WITH SCHEMABINDING
  AS
  SELECT SUM(total_points) AS total_points
    , SUM(available_points) AS available_points
    , SUM(reduction_hours) AS reduction_hours
    , SUM(wpc.contracted_hours) AS contracted_hours
    , wr.effective_from AS effective_from
    , wr.id AS workload_report_id
    , COUNT_BIG(*) AS count
 FROM app.workload_points_calculations AS wpc
    JOIN app.workload AS w ON wpc.workload_id = w.id
    JOIN app.workload_report AS wr ON wpc.workload_report_id = wr.id
    JOIN app.workload_owner AS wo ON wo.id = w.workload_owner_id
    JOIN app.team AS t ON t.id = wo.team_id
    JOIN app.ldu AS l ON l.id = t.ldu_id
    JOIN app.region AS r ON r.id = l.region_id
  WHERE r.description NOT LIKE 'NPS%'
  GROUP BY wr.effective_from, wr.id;

GO

CREATE UNIQUE CLUSTERED INDEX idx_crc_capacity_view ON app.crc_capacity_view (workload_report_id)
