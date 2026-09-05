package com.padhora.service;

import com.padhora.model.Locality;
import com.padhora.repository.LocalityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fills in latitude/longitude for the localities seeded by name in migration V4.
 *
 * <p>This is a deliberate, human-triggered pass rather than something that runs at startup.
 * Coordinates decide the distance sort and the search radius, so a wrong one does not fail
 * loudly - it quietly puts the wrong tutor at the top of a parent's results. Running this
 * on demand means someone is present to read what it could not resolve.
 *
 * <p>Anything not confidently resolved is left NULL with a note explaining why. A locality
 * with no coordinates is still a usable label and autocomplete entry; it just cannot take
 * part in distance ranking. That is a better failure than a plausible wrong answer.
 */
@Service
public class LocalityGeocodingService {

    private static final Logger log = LoggerFactory.getLogger(LocalityGeocodingService.class);

    /**
     * Tricity bounding box. Google will happily return "Sector 17" in another state; a
     * result outside these bounds is rejected rather than stored, because a confidently
     * wrong coordinate is the exact failure this whole approach exists to avoid.
     */
    private static final double MIN_LAT = 30.45, MAX_LAT = 31.00;
    private static final double MIN_LNG = 76.45, MAX_LNG = 77.05;

    private final LocalityRepository localityRepository;
    private final RestClient restClient;

    @Value("${padhora.google.geocoding-key:}")
    private String googleApiKey;

    public LocalityGeocodingService(LocalityRepository localityRepository) {
        this.localityRepository = localityRepository;
        this.restClient = RestClient.create();
    }

    public boolean isConfigured() {
        return googleApiKey != null && !googleApiKey.isBlank();
    }

    /** What one run did. Returned to the admin caller so the outcome is visible, not buried in logs. */
    public record Result(int attempted, int resolved, int rejectedOutOfBounds, int notFound,
                         int failed, List<String> unresolvedNames) {}

    public Result geocodePending(int limit) {
        List<Locality> pending = localityRepository.findByLatitudeIsNullAndActiveTrue();
        if (pending.size() > limit) {
            pending = pending.subList(0, limit);
        }

        int resolved = 0, outOfBounds = 0, notFound = 0, failed = 0;
        List<String> unresolved = new java.util.ArrayList<>();

        for (Locality locality : pending) {
            try {
                Map<String, Object> body = fetch(locality);
                String status = body == null ? "NO_BODY" : String.valueOf(body.get("status"));

                if (!"OK".equals(status)) {
                    locality.setGeocodeNote("Places returned " + status);
                    notFound++;
                    unresolved.add(locality.getName());
                    localityRepository.save(locality);
                    continue;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) body.get("results");
                @SuppressWarnings("unchecked")
                Map<String, Object> geometry = (Map<String, Object>) results.get(0).get("geometry");
                @SuppressWarnings("unchecked")
                Map<String, Object> location = (Map<String, Object>) geometry.get("location");

                double lat = ((Number) location.get("lat")).doubleValue();
                double lng = ((Number) location.get("lng")).doubleValue();

                if (lat < MIN_LAT || lat > MAX_LAT || lng < MIN_LNG || lng > MAX_LNG) {
                    // Almost always a same-named place elsewhere in India. Storing it would
                    // put a tutor hundreds of kilometres from where a parent thinks they are.
                    locality.setGeocodeNote(String.format(
                            "Rejected: %.5f,%.5f is outside Tricity - likely a same-named place elsewhere", lat, lng));
                    outOfBounds++;
                    unresolved.add(locality.getName());
                    localityRepository.save(locality);
                    continue;
                }

                locality.setLatitude(lat);
                locality.setLongitude(lng);
                locality.setGeocodedAt(Instant.now());
                locality.setGeocodeNote(null);
                localityRepository.save(locality);
                resolved++;

            } catch (Exception e) {
                // One bad locality must not abandon the rest of the run.
                log.warn("Geocoding failed for locality {} ({})", locality.getName(), e.toString());
                locality.setGeocodeNote("Lookup failed: " + e.getClass().getSimpleName());
                localityRepository.save(locality);
                failed++;
                unresolved.add(locality.getName());
            }
        }

        log.info("Locality geocoding run: attempted={} resolved={} outOfBounds={} notFound={} failed={}",
                pending.size(), resolved, outOfBounds, notFound, failed);

        return new Result(pending.size(), resolved, outOfBounds, notFound, failed, unresolved);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetch(Locality locality) {
        // The seeded names are already city-qualified ("Sector 5, Panchkula"), but appending
        // the city and country again costs nothing and disambiguates the bare village names.
        String query = locality.getName() + ", " + locality.getCity() + ", India";
        return restClient.get()
                .uri("https://maps.googleapis.com/maps/api/geocode/json?address={q}&key={k}", query, googleApiKey)
                .retrieve()
                .body(HashMap.class);
    }
}
