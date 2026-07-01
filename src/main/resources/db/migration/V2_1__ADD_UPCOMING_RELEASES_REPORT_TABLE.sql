drop table if exists upcoming_releases_reports;

CREATE TABLE upcoming_releases_reports(
    id SERIAL PRIMARY KEY,
    crn VARCHAR NOT NULL,
    person_on_probation VARCHAR NOT NULL,
    expected_release_date DATE NOT NULL,
    prison VARCHAR NOT NULL,
    pdu VARCHAR NOT NULL,
    team VARCHAR NOT NULL,
    probation_practitioner VARCHAR NOT NULL
);
