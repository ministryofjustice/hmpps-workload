drop table if exists reductions_reports;

CREATE TABLE reductions_reports(
    id SERIAL PRIMARY KEY,
    staff_name VARCHAR NOT NULL,
    staff_id VARCHAR NOT NULL,
    staff_team_key VARCHAR NOT NULL,
    reduction_type VARCHAR NOT NULL,
    reduction_hours REAL NOT NULL,
    reduction_status VARCHAR NOT NULL,
    start_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    pdu_code VARCHAR NOT NULL,
    team_code VARCHAR NOT NULL,
    contracted_hours INTEGER NOT NULL
);
