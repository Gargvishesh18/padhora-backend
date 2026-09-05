-- Phase 4: a dedicated identity column for phone-OTP login, separate from the existing
-- `phone` contact field.
--
-- The first version of this migration tried to make `phone` itself unique. That failed
-- against real production data: two existing tutors share the value '7837630824'. `phone`
-- is free text a tutor types into their own profile as contact info - it was never
-- validated for uniqueness and was never safe to repurpose as a login identity.
--
-- auth_phone is nullable, unique, and populated ONLY by the phone-OTP flow
-- (AuthController), always normalised to E.164 (see PhoneUtil). The existing `phone` column
-- is completely untouched by this migration - no data moved, no rows deduplicated, no risk
-- to real tutor contact info.
ALTER TABLE tutors ADD COLUMN auth_phone VARCHAR(20);
ALTER TABLE tutors ADD CONSTRAINT uq_tutors_auth_phone UNIQUE (auth_phone);
