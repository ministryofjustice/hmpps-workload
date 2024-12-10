CREATE INDEX idx_event_manager_created_date_team_code_is_active_created_by
    ON public.event_manager (created_date, created_by, team_code, is_active);

CREATE INDEX idx_case_details_crn_type_tier_first_name_surname
    ON public.case_details (crn, type, tier, first_name, surname);