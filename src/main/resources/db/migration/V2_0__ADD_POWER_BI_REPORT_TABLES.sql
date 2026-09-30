drop table if exists initial_sentence_plan_reports;

CREATE TABLE initial_sentence_plan_reports(
    id SERIAL PRIMARY KEY,
    crn VARCHAR NOT NULL,
    person_on_probation VARCHAR NOT NULL,
    induction_date DATE NOT NULL,
    target_date DATE NOT NULL,
    actions VARCHAR NOT NULL,
    rosh VARCHAR NOT NULL,
    pdu VARCHAR NOT NULL,
    team VARCHAR NOT NULL,
    probation_practitioner VARCHAR NOT NULL
);

drop table if exists hdc_rotl_reports;

CREATE TABLE hdc_rotl_reports(
    id SERIAL PRIMARY KEY,
    crn VARCHAR NOT NULL,
    person_on_probation VARCHAR NOT NULL,
    target_date DATE NOT NULL,
    institutional_report_type VARCHAR NOT NULL,
    actions VARCHAR NOT NULL,
    pdu VARCHAR NOT NULL,
    team VARCHAR NOT NULL,
    probation_practitioner VARCHAR NOT NULL
);

drop table if exists part_b_reports;

CREATE TABLE part_b_reports(
    id SERIAL PRIMARY KEY,
    crn VARCHAR NOT NULL,
    person_on_probation VARCHAR NOT NULL,
    return_to_custody_date DATE NOT NULL,
    target_date DATE NOT NULL,
    tasks VARCHAR NOT NULL,
    actions VARCHAR NOT NULL,
    rarr_type VARCHAR,
    pdu VARCHAR NOT NULL,
    team VARCHAR NOT NULL,
    probation_practitioner VARCHAR NOT NULL
);

drop table if exists part_c_reports;

CREATE TABLE part_c_reports(
    id SERIAL PRIMARY KEY,
    crn VARCHAR NOT NULL,
    person_on_probation VARCHAR NOT NULL,
    target_date DATE NOT NULL,
    notes VARCHAR NOT NULL,
    pdu VARCHAR NOT NULL,
    team VARCHAR NOT NULL,
    probation_practitioner VARCHAR NOT NULL
);

drop table if exists parole_reports;

CREATE TABLE parole_reports(
    id SERIAL PRIMARY KEY,
    crn VARCHAR NOT NULL,
    person_on_probation VARCHAR NOT NULL,
    target_date DATE NOT NULL,
    notes VARCHAR NOT NULL,
    pdu VARCHAR NOT NULL,
    team VARCHAR NOT NULL,
    probation_practitioner VARCHAR NOT NULL
);

drop table if exists reset_reports;

CREATE TABLE reset_reports(
    id SERIAL PRIMARY KEY,
    crn VARCHAR NOT NULL,
    person_on_probation VARCHAR NOT NULL,
    actions VARCHAR NOT NULL,
    notes VARCHAR NOT NULL,
    active_requirements VARCHAR,
    order_category VARCHAR,
    pdu VARCHAR NOT NULL,
    team VARCHAR NOT NULL,
    probation_practitioner VARCHAR NOT NULL
);
