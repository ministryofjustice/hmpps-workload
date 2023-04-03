drop table if exists EVENT_MANAGER_AUDIT;

CREATE TABLE EVENT_MANAGER_AUDIT(
    ID      SERIAL PRIMARY KEY,
    ALLOCATION_JUSTIFICATION_NOTES TEXT NOT NULL,
    SENSITIVE_NOTES BOOLEAN NOT NULL,
    CREATED_BY VARCHAR NOT NULL,
    CREATED_DATE TIMESTAMP WITH TIME ZONE NOT NULL,
    EVENT_MANAGER_ID BIGINT references EVENT_MANAGER(id)
);
