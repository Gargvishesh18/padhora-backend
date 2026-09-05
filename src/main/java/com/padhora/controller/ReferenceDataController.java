package com.padhora.controller;

import com.padhora.model.Grade;
import com.padhora.model.Locality;
import com.padhora.model.Subject;
import com.padhora.repository.GradeRepository;
import com.padhora.repository.LocalityRepository;
import com.padhora.repository.SubjectRepository;
import com.padhora.service.LocalityGeocodingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Read-only reference data (subjects, grades, localities) plus the admin-only geocoding
 * pass that gives localities their coordinates.
 *
 * <p>These lists are what the search form will be built from in Phase 2: locality, then
 * class, then subject. Serving them from the database rather than hardcoding them in the
 * frontend means adding a subject is an INSERT, not a deploy.
 */
@RestController
@RequestMapping("/api")
public class ReferenceDataController {

    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final LocalityRepository localityRepository;
    private final LocalityGeocodingService geocodingService;

    @Value("${padhora.admin-key}")
    private String adminKey;

    public ReferenceDataController(SubjectRepository subjectRepository,
                                   GradeRepository gradeRepository,
                                   LocalityRepository localityRepository,
                                   LocalityGeocodingService geocodingService) {
        this.subjectRepository = subjectRepository;
        this.gradeRepository = gradeRepository;
        this.localityRepository = localityRepository;
        this.geocodingService = geocodingService;
    }

    // Same manual X-Admin-Key check the tutor admin endpoints use. Spring Security's chain
    // permits everything; authorisation for admin routes is enforced here in the controller.
    private boolean isAuthorized(String providedKey) {
        return adminKey != null && !adminKey.isBlank() && adminKey.equals(providedKey);
    }

    @GetMapping("/subjects")
    public List<Subject> subjects() {
        return subjectRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    @GetMapping("/grades")
    public List<Grade> grades() {
        return gradeRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    /**
     * @param city optional filter: Chandigarh, Mohali or Panchkula.
     * @param geocodedOnly when true, returns only localities that can actually take part in
     *                     distance search. Callers that sort by distance should pass true
     *                     rather than silently dropping the rest later.
     */
    @GetMapping("/localities")
    public List<Locality> localities(
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "false") boolean geocodedOnly) {
        List<Locality> all = (city == null || city.isBlank())
                ? localityRepository.findByActiveTrueOrderByCityAscNameAsc()
                : localityRepository.findByCityAndActiveTrueOrderByNameAsc(city);
        return geocodedOnly ? all.stream().filter(Locality::isGeocoded).toList() : all;
    }

    /** How much of the locality table can actually be used for distance search yet. */
    @GetMapping("/admin/localities/geocode-status")
    public ResponseEntity<?> geocodeStatus(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        if (!isAuthorized(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        long total = localityRepository.count();
        long geocoded = localityRepository.countByLatitudeIsNotNull();
        List<Locality> pending = localityRepository.findByLatitudeIsNullAndActiveTrue();
        return ResponseEntity.ok(Map.of(
                "total", total,
                "geocoded", geocoded,
                "pending", total - geocoded,
                "configured", geocodingService.isConfigured(),
                "pendingNames", pending.stream().map(Locality::getName).limit(200).toList()
        ));
    }

    /**
     * Resolves coordinates for localities that do not have them yet. Safe to run repeatedly:
     * it only touches rows where latitude IS NULL, so a second run retries exactly the ones
     * that failed and leaves the rest alone.
     */
    @PostMapping("/admin/localities/geocode")
    public ResponseEntity<?> geocode(
            @RequestHeader(value = "X-Admin-Key", required = false) String key,
            @RequestParam(defaultValue = "200") int limit) {
        if (!isAuthorized(key)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        if (!geocodingService.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "error", "No geocoding key configured. Set PADHORA_GOOGLE_GEOCODING_KEY."));
        }
        return ResponseEntity.ok(geocodingService.geocodePending(limit));
    }
}
