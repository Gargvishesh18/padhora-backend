package com.padhora.service;

import com.padhora.model.Tutor;
import com.padhora.repository.TutorRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * The one place the published ranking formula lives:
 *
 * <pre>0.50 * proximity + 0.20 * verified + 0.20 * response_rate + 0.10 * completeness</pre>
 *
 * <p>It is fixed and never for sale (see the roadmap) - which only means something if it is
 * computed in exactly one place. Before this, the frontend computed its own distance sort in
 * JavaScript; that math now lives here, and the frontend renders whatever order this returns.
 *
 * <p>Filtering and ranking happen in application code rather than in SQL. At current and
 * near-term scale (dozens of tutors, not thousands) that costs nothing measurable and keeps
 * the ranking formula in ordinary, readable Java instead of a harder-to-audit SQL expression.
 * Revisit this once search results routinely run into the hundreds.
 */
@Service
public class TutorSearchService {

    // The largest radius button in the UI (2 / 5 / 10 km). A tutor at this distance or
    // beyond earns no proximity credit at all; the term degrades linearly to that point.
    // Keep this in sync with the frontend's radius options - see index.html.
    private static final double PROXIMITY_HORIZON_KM = 10.0;

    // A tutor nobody has enquired about yet has not failed to respond - null response_rate
    // must not be scored as if it were 0. Scoring it as "average" (neither help nor hurt)
    // is the least biased default until Phase 6 gives us a real number.
    private static final double DEFAULT_RESPONSE_RATE = 0.5;

    private final TutorRepository tutorRepository;

    public TutorSearchService(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    public static class Params {
        public String gradeSlug;
        public String subjectSlug;
        public String mode;
        public String type;
        public String area; // legacy - superseded by locality/gradeSlug/subjectSlug, kept for old callers
        public Double lat;
        public Double lng;
        public Double radiusKm; // null = no distance cutoff, still ranked by proximity if lat/lng given
    }

    public List<Tutor> search(Params p) {
        List<Tutor> candidates = tutorRepository.search(p.area, p.mode, p.type, p.gradeSlug, p.subjectSlug);

        boolean hasLocation = p.lat != null && p.lng != null;
        if (!hasLocation) {
            // No location to rank by proximity against - fall back to newest-first, which is
            // also what callers with no filters at all (e.g. the homepage tutor count) expect.
            return candidates.stream()
                    .sorted(Comparator.comparing(Tutor::getCreatedAt).reversed())
                    .toList();
        }

        return candidates.stream()
                .map(t -> withDistanceAndScore(t, p.lat, p.lng))
                // A radius filter means the parent explicitly asked "within X km" - a tutor we
                // cannot place on the map has not been shown to be within it, so a radius
                // cutoff excludes them rather than exempting them. With no radius requested,
                // an un-locatable tutor still appears (ranked last, via proximity = 0 above)
                // rather than disappearing from a plain "browse everyone" list.
                .filter(t -> p.radiusKm == null || (t.getDistanceKm() != null && t.getDistanceKm() <= p.radiusKm))
                .sorted(Comparator.comparing(
                        (Tutor t) -> t.getRankScore() == null ? Double.NEGATIVE_INFINITY : t.getRankScore())
                        .reversed())
                .toList();
    }

    private Tutor withDistanceAndScore(Tutor t, double parentLat, double parentLng) {
        Double distanceKm = (t.getLatitude() != null && t.getLongitude() != null)
                ? haversineKm(parentLat, parentLng, t.getLatitude(), t.getLongitude())
                : null;
        t.setDistanceKm(distanceKm);
        t.setRankScore(score(t, distanceKm));
        return t;
    }

    private double score(Tutor t, Double distanceKm) {
        double proximity = distanceKm == null
                ? 0.0 // no coordinates on file - never let an un-locatable listing outrank a locatable one
                : clamp01(1.0 - (distanceKm / PROXIMITY_HORIZON_KM));
        double verified = t.isVerified() ? 1.0 : 0.0;
        double responseRate = t.getResponseRate() != null ? clamp01(t.getResponseRate()) : DEFAULT_RESPONSE_RATE;
        double completeness = clamp01(t.getCompletenessScore() / 100.0);

        return 0.50 * proximity + 0.20 * verified + 0.20 * responseRate + 0.10 * completeness;
    }

    private double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    // Same formula the frontend used to compute client-side (see haversineKm in index.html,
    // being retired now that the backend is the single source of truth for it).
    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
