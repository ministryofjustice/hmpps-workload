alter table initial_sentence_plan_reports drop column crn;
alter table initial_sentence_plan_reports drop column person_on_probation;
alter table initial_sentence_plan_reports drop column induction_date;
alter table initial_sentence_plan_reports drop column actions;
alter table initial_sentence_plan_reports drop column rosh;
alter table initial_sentence_plan_reports drop column pdu;

alter table hdc_rotl_reports drop column crn;
alter table hdc_rotl_reports drop column person_on_probation;
alter table hdc_rotl_reports drop column institutional_report_type;
alter table hdc_rotl_reports drop column actions;
alter table hdc_rotl_reports drop column pdu;

alter table part_b_reports drop column crn;
alter table part_b_reports drop column person_on_probation;
alter table part_b_reports drop column return_to_custody_date;
alter table part_b_reports drop column tasks;
alter table part_b_reports drop column actions;
alter table part_b_reports drop column rarr_type;
alter table part_b_reports drop column pdu;

alter table part_c_reports drop column crn;
alter table part_c_reports drop column person_on_probation;
alter table part_c_reports drop column notes;
alter table part_c_reports drop column pdu;

alter table parole_reports drop column crn;
alter table parole_reports drop column person_on_probation;
alter table parole_reports drop column notes;
alter table parole_reports drop column pdu;

alter table reset_reports drop column crn;
alter table reset_reports drop column person_on_probation;
alter table reset_reports drop column actions;
alter table reset_reports drop column notes;
alter table reset_reports drop column active_requirements;
alter table reset_reports drop column order_category;
alter table reset_reports drop column pdu;

alter table upcoming_releases_reports drop column crn;
alter table upcoming_releases_reports drop column person_on_probation;
alter table upcoming_releases_reports drop column prison;
alter table upcoming_releases_reports drop column pdu;