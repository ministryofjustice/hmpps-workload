alter table staging.arms alter column id set generated by default;
alter table staging.cms alter column id set generated by default;
alter table staging.court_reporters alter column id set generated by default;
alter table staging.court_reports alter column id set generated by default;
alter table staging.flag_o_due alter column id set generated by default;
alter table staging.flag_priority alter column id set generated by default;
alter table staging.flag_upw alter column id set generated by default;
alter table staging.flag_warr_4_n alter column id set generated by default;
alter table staging.gs alter column id set generated by default;
alter table staging.included_excluded alter column id set generated by default;
alter table staging.inst_reports alter column id set generated by default;
alter table staging.knex_migrations alter column id set generated by default;
alter table staging.omic alter column id set generated by default;
alter table staging.omic_teams alter column id set generated by default;
alter table staging.suspended_lifers alter column id set generated by default;
alter table staging.t2a alter column id set generated by default;
alter table staging.t2a_detail alter column id set generated by default;
alter table staging.wmt_extract alter column id set generated by default;
alter table staging.wmt_extract_filtered alter column id set generated by default;
alter table staging.wmt_extract_sa alter column id set generated by default;
alter table app.adjustment_category alter column id set generated by default;
alter table app.case_category alter column id set generated by default;
alter table app.adjustment_reason alter column id set generated by default;
alter table app.adjustments alter column id set generated by default;
alter table app.export_file alter column id set generated by default;
alter table app.knex_migrations alter column id set generated by default;
alter table app.row_type_definitions alter column id set generated by default;
alter table app.workload alter column id set generated by default;
alter table app.case_details alter column id set generated by default;
alter table app.workload_owner alter column id set generated by default;
alter table app.court_reports alter column id set generated by default;
alter table app.court_reports_calculations alter column id set generated by default;
alter table app.workload_points alter column id set generated by default;
alter table app.workload_report alter column id set generated by default;
alter table app.region alter column id set generated by default;
alter table app.ldu alter column id set generated by default;
alter table app.offender_manager_type alter column id set generated by default;
alter table app.offender_manager alter column id set generated by default;
alter table app.omic_workload alter column id set generated by default;
alter table app.omic_case_details alter column id set generated by default;
alter table app.omic_tiers alter column id set generated by default;
alter table app.omic_workload_points_calculations alter column id set generated by default;
alter table app.reduction_category alter column id set generated by default;
alter table app.reduction_reason alter column id set generated by default;
alter table app.reductions alter column id set generated by default;
alter table app.reductions_history alter column id set generated by default;
alter table app.tasks alter column id set generated by default;
alter table app.team alter column id set generated by default;
alter table app.tiers alter column id set generated by default;
alter table app.roles alter column id set generated by default;
alter table app.user_role alter column id set generated by default;
alter table app.users alter column id set generated by default;
alter table app.workload_points_calculations alter column id set generated by default;