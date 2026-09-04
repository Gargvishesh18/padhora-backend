-- Phase 1: backfill the normalised join tables from the existing free-text data.
--
-- Without this, tutor_subjects and tutor_grades are empty and every tutor already on the
-- platform disappears from a Phase 2 subject/grade search. The source rows in
-- tutor_grade_subjects are left untouched, so anything mapped wrongly here can be
-- inspected and corrected against the original.
--
-- Anything that does not map is skipped rather than guessed at. To find those afterwards:
--
--   SELECT t.id, t.name, tgs.grade, tgs.subjects
--     FROM tutors t JOIN tutor_grade_subjects tgs ON tgs.tutor_id = t.id
--    WHERE NOT EXISTS (SELECT 1 FROM tutor_subjects ts WHERE ts.tutor_id = t.id)
--       OR NOT EXISTS (SELECT 1 FROM tutor_grades tg WHERE tg.tutor_id = t.id);
--
-- Supply is small at this stage, so the handful this misses is a few minutes of manual
-- fixing rather than a reason to write a cleverer parser.

-- ---------------------------------------------------------------------------
-- Subjects: "Math, Science" -> two rows.
-- ---------------------------------------------------------------------------
INSERT INTO tutor_subjects (tutor_id, subject_id)
SELECT DISTINCT e.tutor_id, s.id
  FROM (
        SELECT tgs.tutor_id, lower(btrim(part)) AS raw_subject
          FROM tutor_grade_subjects tgs
          CROSS JOIN LATERAL unnest(string_to_array(coalesce(tgs.subjects, ''), ',')) AS part
         WHERE btrim(part) <> ''
       ) e
  JOIN (VALUES
            ('math',                    'Mathematics'),
            ('maths',                   'Mathematics'),
            ('mathematics',             'Mathematics'),
            ('english',                 'English'),
            ('eng',                     'English'),
            ('hindi',                   'Hindi'),
            ('punjabi',                 'Punjabi'),
            ('science',                 'Science'),
            ('sci',                     'Science'),
            ('evs',                     'EVS'),
            ('environmental studies',   'EVS'),
            ('environment studies',     'EVS'),
            ('social studies',          'Social Studies'),
            ('social science',          'Social Studies'),
            ('sst',                     'Social Studies'),
            ('sanskrit',                'Sanskrit'),
            ('computer',                'Computer Science'),
            ('computers',               'Computer Science'),
            ('computer science',        'Computer Science'),
            ('gk',                      'General Knowledge'),
            ('general knowledge',       'General Knowledge'),
            ('handwriting',             'Handwriting'),
            ('drawing',                 'Drawing & Art'),
            ('art',                     'Drawing & Art'),
            ('drawing & art',           'Drawing & Art'),
            ('drawing and art',         'Drawing & Art')
       ) AS alias(written, canonical) ON alias.written = e.raw_subject
  JOIN subjects s ON s.name = alias.canonical
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Grades. Two shapes in the wild:
--   named    -- "Nursery", "LKG", "UKG"
--   numeric  -- "Class 5", and ranges like "Class 6 - Class 8"
-- ---------------------------------------------------------------------------
INSERT INTO tutor_grades (tutor_id, grade_id)
SELECT DISTINCT u.tutor_id, u.grade_id
  FROM (
        -- Named pre-school grades, matched as substrings so "Nursery & LKG" yields both.
        SELECT g.tutor_id, gr.id AS grade_id
          FROM (
                SELECT tutor_id, lower(btrim(grade)) AS gl
                  FROM tutor_grade_subjects
                 WHERE grade IS NOT NULL AND btrim(grade) <> ''
               ) g
          JOIN (VALUES
                    ('nursery', 'Nursery'),
                    ('lkg',     'LKG'),
                    ('l.k.g',   'LKG'),
                    ('ukg',     'UKG'),
                    ('u.k.g',   'UKG')
               ) AS alias(written, canonical) ON g.gl LIKE '%' || alias.written || '%'
          JOIN grades gr ON gr.name = alias.canonical

        UNION ALL

        -- Numeric grades. A single number is that grade; two or more numbers are treated
        -- as an inclusive range between the lowest and highest, which is how the free-text
        -- field was actually being used ("Class 6 - Class 8").
        --
        -- Bounded to 1-12 so a stray number in the text (a year, a phone fragment) cannot
        -- generate a runaway series. Numbers above Class 8 simply match no grade row,
        -- which is the correct outcome for our Nursery-8 scope.
        SELECT n.tutor_id, gr.id AS grade_id
          FROM (
                SELECT g.tutor_id,
                       (SELECT min(v) FROM (SELECT (m[1])::int AS v
                                              FROM regexp_matches(g.gl, '(\d+)', 'g') AS m) q) AS lo,
                       (SELECT max(v) FROM (SELECT (m[1])::int AS v
                                              FROM regexp_matches(g.gl, '(\d+)', 'g') AS m) q) AS hi
                  FROM (
                        SELECT tutor_id, lower(btrim(grade)) AS gl
                          FROM tutor_grade_subjects
                         WHERE grade IS NOT NULL AND btrim(grade) <> ''
                       ) g
               ) n
          CROSS JOIN LATERAL generate_series(n.lo, n.hi) AS num
          JOIN grades gr ON gr.slug = 'class-' || num
         WHERE n.lo BETWEEN 1 AND 12
           AND n.hi BETWEEN 1 AND 12
       ) u
ON CONFLICT DO NOTHING;

DO $$
DECLARE
    subject_links INTEGER;
    grade_links   INTEGER;
    unmapped      INTEGER;
BEGIN
    SELECT count(*) INTO subject_links FROM tutor_subjects;
    SELECT count(*) INTO grade_links   FROM tutor_grades;
    SELECT count(DISTINCT tgs.tutor_id) INTO unmapped
      FROM tutor_grade_subjects tgs
     WHERE NOT EXISTS (SELECT 1 FROM tutor_subjects ts WHERE ts.tutor_id = tgs.tutor_id)
        OR NOT EXISTS (SELECT 1 FROM tutor_grades tg WHERE tg.tutor_id = tgs.tutor_id);

    RAISE NOTICE 'Backfill complete: % tutor-subject links, % tutor-grade links, % tutors still unmapped.',
                 subject_links, grade_links, unmapped;
END $$;
