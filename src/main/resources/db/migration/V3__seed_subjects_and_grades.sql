-- Phase 1: seed subjects and grades.
--
-- Grade scope is Nursery to Class 8, decided 2026-09-04. Padhora is positioned as a
-- primary/middle-years tutor marketplace: narrower scope means we can plausibly reach
-- "no target locality returns zero for Maths or English at any grade", which is a launch
-- condition. Class 9-12 is deliberately out of scope for now — adding it later is an
-- INSERT, so nothing here forecloses it.

INSERT INTO grades (name, slug, sort_order) VALUES
    ('Nursery',  'nursery',  10),
    ('LKG',      'lkg',      20),
    ('UKG',      'ukg',      30),
    ('Class 1',  'class-1',  40),
    ('Class 2',  'class-2',  50),
    ('Class 3',  'class-3',  60),
    ('Class 4',  'class-4',  70),
    ('Class 5',  'class-5',  80),
    ('Class 6',  'class-6',  90),
    ('Class 7',  'class-7', 100),
    ('Class 8',  'class-8', 110);

-- Subjects a Tricity primary/middle parent actually asks for. Punjabi and Hindi are
-- first-class here, not afterthoughts — they are compulsory in most schools in the region
-- and are a common reason a parent goes looking for a tutor at all.
INSERT INTO subjects (name, slug, sort_order) VALUES
    ('Mathematics',            'mathematics',            10),
    ('English',                'english',                20),
    ('Hindi',                  'hindi',                  30),
    ('Punjabi',                'punjabi',                40),
    ('Science',                'science',                50),
    ('EVS',                    'evs',                    60),
    ('Social Studies',         'social-studies',         70),
    ('Sanskrit',               'sanskrit',               80),
    ('Computer Science',       'computer-science',       90),
    ('General Knowledge',      'general-knowledge',     100),
    ('Handwriting',            'handwriting',           110),
    ('Drawing & Art',          'drawing-and-art',       120);
