drop table if exists CASE_DETAILS;

CREATE TABLE CASE_DETAILS(
   CREATED_DATE TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('utc', now()),
   CRN VARCHAR NOT NULL,
   TYPE VARCHAR NOT NULL,
   TIER VARCHAR NOT NULL,
   PRIMARY KEY(CREATED_DATE, CRN, TYPE, TIER)
);