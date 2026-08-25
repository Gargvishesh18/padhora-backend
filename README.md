# Padhora Backend

Spring Boot REST API for Padhora. Endpoints:

- `GET /api/tutors?area=&mode=&type=` — search approved listings
- `GET /api/tutors/{id}` — one tutor's detail
- `POST /api/tutors` — submit a new listing (goes in as PENDING)
- `GET /api/tutors/admin/pending` — list all pending listings (no auth yet — see below)
- `PATCH /api/tutors/admin/{id}/approve` — approve a listing
- `PATCH /api/tutors/admin/{id}/reject` — reject a listing

## Before real launch (not done yet)
- The `/admin/*` endpoints have **no authentication**. Fine for you testing alone;
  do not launch publicly until these are locked behind a login. Flag this back to
  me when you're ready for that step and I'll add it.
- CORS is wide open (`allowedOrigins("*")`). Once you have your real Vercel URL,
  I'll narrow this down.

## Deploy to Railway (step-by-step comes separately once you're ready to deploy)
