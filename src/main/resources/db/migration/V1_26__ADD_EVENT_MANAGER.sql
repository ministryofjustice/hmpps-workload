drop table if exists EVENT_MANAGER;

CREATE TABLE EVENT_MANAGER(
    ID      SERIAL PRIMARY KEY,
    UUID    UUID      NOT NULL,
    CRN VARCHAR NOT NULL,
    STAFF_ID BIGINT NOT NULL,
    EVENT_ID BIGINT NOT NULL,
    STAFF_CODE VARCHAR NOT NULL,
    TEAM_ID BIGINT,
    TEAM_CODE VARCHAR NOT NULL,
    CREATED_BY VARCHAR NOT NULL,
    CREATED_DATE TIMESTAMP WITH TIME ZONE NOT NULL
);