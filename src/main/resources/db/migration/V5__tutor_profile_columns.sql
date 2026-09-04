-- Phase 1: new tutor profile columns.
--
-- Every column here is nullable or defaulted, because this runs against a live database
-- with real tutor rows in it. Nothing is backfilled with a guess.

ALTER TABLE tutors
    ADD COLUMN locality_id        BIGINT,
    -- Fee as a range rather than the existing single `price`. `price`/`price_type`/
    -- `price_unit` stay for now: they are what the live site reads, and dropping them
    -- would break production the moment this deploys. Phase 2 moves reads across, and a
    -- later migration can retire them once nothing depends on them.
    ADD COLUMN fee_min            INTEGER,
    ADD COLUMN fee_max            INTEGER,
    -- 0-100. Drives the 0.10 completeness term in the published ranking formula and tells
    -- a tutor what is still missing from their listing.
    ADD COLUMN completeness_score INTEGER NOT NULL DEFAULT 0,
    -- 0.0-1.0, or NULL for "not enough enquiries to say". NULL is not the same as zero and
    -- must not be ranked as if it were: a new tutor has not failed to respond, they have
    -- simply not been asked yet.
    ADD COLUMN response_rate      DOUBLE PRECISION,
    -- The public "Verified" badge renders only when this is set. Approval status alone is
    -- not verification.
    ADD COLUMN verified_at        TIMESTAMP(6) WITH TIME ZONE;

ALTER TABLE tutors
    ADD CONSTRAINT fk_tutors_locality FOREIGN KEY (locality_id) REFERENCES localities (id);

ALTER TABLE tutors
    ADD CONSTRAINT ck_tutors_fee_range CHECK (
        fee_min IS NULL OR fee_max IS NULL OR fee_min <= fee_max
    ),
    ADD CONSTRAINT ck_tutors_fee_nonneg CHECK (
        (fee_min IS NULL OR fee_min >= 0) AND (fee_max IS NULL OR fee_max >= 0)
    ),
    ADD CONSTRAINT ck_tutors_completeness CHECK (completeness_score BETWEEN 0 AND 100),
    ADD CONSTRAINT ck_tutors_response_rate CHECK (response_rate IS NULL OR (response_rate BETWEEN 0 AND 1));

CREATE INDEX idx_tutors_locality ON tutors (locality_id);
CREATE INDEX idx_tutors_status ON tutors (status);

-- Add PAUSED to the status check. A tutor who is full or away needs a way to stop
-- receiving enquiries that is not "delete my listing" or "get rejected".
--
-- The existing constraint was created inline by Hibernate, so Postgres named it. The name
-- should be tutors_status_check, but this database was built by ddl-auto=update over time
-- and that is worth not betting on — look the constraint up rather than assuming its name.
DO $$
DECLARE
    existing_name TEXT;
BEGIN
    SELECT conname INTO existing_name
      FROM pg_constraint
     WHERE conrelid = 'tutors'::regclass
       AND contype = 'c'
       AND pg_get_constraintdef(oid) ILIKE '%status%';

    IF existing_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE tutors DROP CONSTRAINT %I', existing_name);
    END IF;
END $$;

ALTER TABLE tutors
    ADD CONSTRAINT tutors_status_check CHECK (
        status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'PAUSED')
    );
