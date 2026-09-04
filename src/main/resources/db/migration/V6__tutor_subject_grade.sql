-- Phase 1: normalised "what does this tutor teach" join tables.
--
-- These replace, for search purposes, the free-text tutor_grade_subjects table
-- (grade "Class 6 - Class 8", subjects "Math, Science"). That table is NOT dropped here:
-- it is what the live site currently reads to render a listing, and it holds the only copy
-- of some tutors' data. V7 backfills from it; Phase 2 moves reads across; a later
-- migration retires it once nothing depends on it.

CREATE TABLE tutor_subjects (
    tutor_id   BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    CONSTRAINT tutor_subjects_pkey PRIMARY KEY (tutor_id, subject_id),
    CONSTRAINT fk_tutor_subjects_tutor   FOREIGN KEY (tutor_id)   REFERENCES tutors (id) ON DELETE CASCADE,
    CONSTRAINT fk_tutor_subjects_subject FOREIGN KEY (subject_id) REFERENCES subjects (id)
);

CREATE TABLE tutor_grades (
    tutor_id BIGINT NOT NULL,
    grade_id BIGINT NOT NULL,
    CONSTRAINT tutor_grades_pkey PRIMARY KEY (tutor_id, grade_id),
    CONSTRAINT fk_tutor_grades_tutor FOREIGN KEY (tutor_id) REFERENCES tutors (id) ON DELETE CASCADE,
    CONSTRAINT fk_tutor_grades_grade FOREIGN KEY (grade_id) REFERENCES grades (id)
);

-- Search asks "who teaches subject X / grade Y", i.e. it looks these up from the reference
-- side. The primary keys above only index tutor_id first, so add the reverse.
CREATE INDEX idx_tutor_subjects_subject ON tutor_subjects (subject_id);
CREATE INDEX idx_tutor_grades_grade ON tutor_grades (grade_id);
