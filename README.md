# Padhora Backend

Spring Boot 3.3.4 / Java 17 / PostgreSQL. Deployed on Railway at
`https://padhora-backend-production.up.railway.app`.

## Schema is owned by Flyway

Migrations live in `src/main/resources/db/migration`. `spring.jpa.hibernate.ddl-auto` is
`validate`, so **the app refuses to start if the entities and the database disagree**. That
is deliberate: it used to be `update`, which let Hibernate silently alter production tables.

The Railway database predates Flyway. `baseline-on-migrate` stamps that existing schema as
version 1 and applies V2 onward, so `V1__baseline.sql` never runs against production — it
only builds a fresh database. Both paths were verified to produce byte-identical schemas.

**To change the schema: add a new `V<n>__description.sql`.** Never edit an applied
migration, and never reach for `ddl-auto=update` to "just add a column".

## Endpoints

Tutors:
- `GET /api/tutors?area=&mode=&type=` — search approved listings
- `GET /api/tutors/{id}` — one tutor's detail
- `POST /api/tutors` — submit a listing (enters as PENDING)

Reference data (drives the Phase 2 search form: locality → class → subject):
- `GET /api/subjects`
- `GET /api/grades`
- `GET /api/localities?city=&geocodedOnly=`

Enquiries and analytics: see `EnquiryController` and `AnalyticsController`.

Admin — all require the `X-Admin-Key` header:
- `GET /api/tutors/admin/pending`, `PATCH /api/tutors/admin/{id}/approve|reject`
- `GET /api/admin/localities/geocode-status`
- `POST /api/admin/localities/geocode?limit=200`

## Locality coordinates need one manual pass

The ~153 Tricity localities are seeded **by name only**, with `latitude`/`longitude` NULL.
Coordinates drive the distance sort and the search radius, so a guessed one does not fail
loudly — it quietly puts the wrong tutor at the top of a parent's results, with no way to
tell which of 153 rows is wrong.

So they are resolved by an explicit pass:

1. Create a Google API key restricted to the **Geocoding API** (server-side, IP-restricted;
   not the browser key the frontend uses). Set it in Railway as
   `PADHORA_GOOGLE_GEOCODING_KEY`.
2. `curl -X POST -H "X-Admin-Key: $KEY" $BASE/api/admin/localities/geocode`
3. Read the response. It reports what resolved and names everything that did not.
4. Re-run as needed — it only touches rows where latitude IS NULL, so it retries failures
   and leaves resolved rows alone.

Results outside a Tricity bounding box are **rejected**, not stored: Google will happily
return a same-named "Sector 17" in another state. Anything unresolved keeps
`geocode_note` explaining why, and stays usable as a label — it just cannot take part in
distance ranking until it has coordinates.

## Privacy: what we never store

`verifications` holds the evidence behind the public "Verified" badge. It stores the ID
**type**, its **last four digits**, and whether the name matched. It never stores ID images
or full ID numbers — not encrypted, not hashed, not temporarily. Holding that data makes
Padhora a custodian of sensitive personal data under the DPDP Act and buys nothing that a
human looking at the document during the verification call does not already provide.

This is enforced by the database, not by convention: `id_last4` is `VARCHAR(4)` with a
`^[0-9]{4}$` CHECK, and `ck_verifications_complete` refuses to set `verified_at` unless the
phone check, ID check, name match, locality confirmation and verification call are all
recorded. Do not relax either constraint.

## Configuration

Set these in Railway's Variables tab. Defaults in `application.properties` are for local
development only.

| Variable | Purpose |
| --- | --- |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | Injected by the Postgres plugin |
| `PADHORA_ADMIN_KEY` | Shared secret for `X-Admin-Key` on admin routes |
| `PADHORA_JWT_SECRET` | Signs tutor login tokens; base64 decoding to ≥32 bytes |
| `PADHORA_OTP_STUB_MODE` | `true` echoes OTPs in the API response. Must be false/unset once real parents use the site |
| `PADHORA_GOOGLE_GEOCODING_KEY` | Server-side key for the locality geocoding pass |

Admin auth and CORS **are** implemented, contrary to what this README said for a long time:
admin routes check `X-Admin-Key` in the controller, and CORS is pinned to the Vercel origin
in `WebConfig`. Spring Security's filter chain permits all requests; authorisation for admin
routes is enforced in the controllers, not via `authorizeHttpRequests`.

## Local development

```bash
# Postgres on :5432 with a `padhora` database, then:
mvn spring-boot:run
```

Flyway builds the whole schema from empty on first run.
