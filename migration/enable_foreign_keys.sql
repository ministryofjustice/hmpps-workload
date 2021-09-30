ALTER TABLE "app"."workload_points_calculations" ADD CONSTRAINT "workload_points_calculations_workload_report_id_foreign_1565248" FOREIGN KEY (workload_report_id) REFERENCES app.workload_report(id);
ALTER TABLE "app"."workload_points_calculations" ADD CONSTRAINT "workload_points_calculations_workload_points_id_foreign_1549248" FOREIGN KEY (workload_points_id) REFERENCES app.workload_points(id);
ALTER TABLE "app"."workload_points_calculations" ADD CONSTRAINT "workload_points_calculations_workload_id_foreign_1533248517" FOREIGN KEY (workload_id) REFERENCES app.workload(id);
ALTER TABLE "app"."workload_points_calculations" ADD CONSTRAINT "workload_points_calculations_t2a_workload_points_id_foreign_151" FOREIGN KEY (t2a_workload_points_id) REFERENCES app.workload_points(id);
ALTER TABLE "app"."workload_owner" ADD CONSTRAINT "workload_owner_team_id_foreign_509244869" FOREIGN KEY (team_id) REFERENCES app.team(id);
ALTER TABLE "app"."workload_owner" ADD CONSTRAINT "workload_owner_offender_manager_id_foreign_493244812" FOREIGN KEY (offender_manager_id) REFERENCES app.offender_manager(id);
ALTER TABLE "app"."workload" ADD CONSTRAINT "workload_workload_owner_id_foreign_1389248004" FOREIGN KEY (workload_owner_id) REFERENCES app.workload_owner(id);
ALTER TABLE "app"."tiers" ADD CONSTRAINT "tiers_workload_id_foreign_1981250113" FOREIGN KEY (workload_id) REFERENCES app.workload(id);
ALTER TABLE "app"."team" ADD CONSTRAINT "team_ldu_id_foreign_365244356" FOREIGN KEY (ldu_id) REFERENCES app.ldu(id);
ALTER TABLE "app"."reductions" ADD CONSTRAINT "reductions_workload_owner_id_foreign_1085246921" FOREIGN KEY (workload_owner_id) REFERENCES app.workload_owner(id);
ALTER TABLE "app"."reductions" ADD CONSTRAINT "reductions_reduction_reason_id_foreign_1069246864" FOREIGN KEY (reduction_reason_id) REFERENCES app.reduction_reason(id);
ALTER TABLE "app"."reduction_reason" ADD CONSTRAINT "reduction_reason_category_id_foreign_237243900" FOREIGN KEY (category_id) REFERENCES app.reduction_category(id);
ALTER TABLE "app"."omic_workload_points_calculations" ADD CONSTRAINT "fk__omic_work__workl__5cb86648_989246579" FOREIGN KEY (workload_points_id) REFERENCES app.workload_points(id);
ALTER TABLE "app"."omic_workload_points_calculations" ADD CONSTRAINT "fk__omic_work__workl__5bc4420f_973246522" FOREIGN KEY (workload_report_id) REFERENCES app.workload_report(id);
ALTER TABLE "app"."omic_workload_points_calculations" ADD CONSTRAINT "fk__omic_work__t2a_w__5f94d2f3_957246465" FOREIGN KEY (t2a_workload_points_id) REFERENCES app.workload_points(id);
ALTER TABLE "app"."omic_workload_points_calculations" ADD CONSTRAINT "fk__omic_work__omic___5dac8a81_941246408" FOREIGN KEY (omic_workload_id) REFERENCES app.omic_workload(id);
ALTER TABLE "app"."omic_workload" ADD CONSTRAINT "fk__omic_work__workl__42f89445_861246123" FOREIGN KEY (workload_owner_id) REFERENCES app.workload_owner(id);
ALTER TABLE "app"."omic_tiers" ADD CONSTRAINT "fk__omic_tier__omic___4f5e6b2a_1821249543" FOREIGN KEY (omic_workload_id) REFERENCES app.omic_workload(id);
ALTER TABLE "app"."offender_manager" ADD CONSTRAINT "offender_manager_type_id_foreign_173243672" FOREIGN KEY (type_id) REFERENCES app.offender_manager_type(id);
ALTER TABLE "app"."ldu" ADD CONSTRAINT "ldu_region_id_foreign_109243444" FOREIGN KEY (region_id) REFERENCES app.region(id);
ALTER TABLE "app"."court_reports_calculations" ADD CONSTRAINT "court_reports_calculations_workload_report_id_foreign_653245382" FOREIGN KEY (workload_report_id) REFERENCES app.workload_report(id);
ALTER TABLE "app"."court_reports_calculations" ADD CONSTRAINT "court_reports_calculations_workload_points_id_foreign_637245325" FOREIGN KEY (workload_points_id) REFERENCES app.workload_points(id);
ALTER TABLE "app"."court_reports_calculations" ADD CONSTRAINT "court_reports_calculations_court_reports_id_foreign_621245268" FOREIGN KEY (court_reports_id) REFERENCES app.court_reports(id);
ALTER TABLE "app"."court_reports" ADD CONSTRAINT "court_reports_workload_owner_id_foreign_557245040" FOREIGN KEY (workload_owner_id) REFERENCES app.workload_owner(id);
ALTER TABLE "app"."case_details" ADD CONSTRAINT "case_details_workload_id_foreign_1613248802" FOREIGN KEY (workload_id) REFERENCES app.workload(id);
ALTER TABLE "app"."adjustments" ADD CONSTRAINT "adjustments_reason_id_foreign_07092021" FOREIGN KEY (adjustment_reason_id) REFERENCES app.adjustment_reason(id);
ALTER TABLE "app"."user_role" ADD CONSTRAINT "user_role_user_id_foreign_445244641" FOREIGN KEY (user_id) REFERENCES app.users(id);
ALTER TABLE "app"."user_role" ADD CONSTRAINT "user_role_role_id_foreign_429244584" FOREIGN KEY (role_id) REFERENCES app.roles(id);
ALTER TABLE "app"."tasks" ADD CONSTRAINT "tasks_workload_report_id_foreign_301244128" FOREIGN KEY (workload_report_id) REFERENCES app.workload_report(id);
ALTER TABLE "app"."reductions_history" ADD CONSTRAINT "fk__reduction__reduc__2cb44d6b_1181247263" FOREIGN KEY (reduction_reason_id) REFERENCES app.reduction_reason(id);
ALTER TABLE "app"."reductions_history" ADD CONSTRAINT "fk__reduction__reduc__2acc04f9_1165247206" FOREIGN KEY (reduction_id) REFERENCES app.reductions(id);
ALTER TABLE "app"."omic_case_details" ADD CONSTRAINT "fk__omic_case__omic___58e7d564_1661248973" FOREIGN KEY (omic_workload_id) REFERENCES app.omic_workload(id);