package com.padhora.controller;

import com.padhora.dto.TutorRequest;
import com.padhora.model.Grade;
import com.padhora.model.Subject;
import com.padhora.model.Tutor;
import com.padhora.repository.GradeRepository;
import com.padhora.repository.SubjectRepository;
import com.padhora.repository.TutorRepository;
import com.padhora.service.TutorCompletenessService;
import com.padhora.service.TutorSearchService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/tutors")
public class TutorController {

    private final TutorRepository tutorRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;
    private final TutorSearchService tutorSearchService;
    private final TutorCompletenessService tutorCompletenessService;

    @Value("${padhora.admin-key}")
    private String adminKey;

    public TutorController(TutorRepository tutorRepository,
                            SubjectRepository subjectRepository,
                            GradeRepository gradeRepository,
                            TutorSearchService tutorSearchService,
                            TutorCompletenessService tutorCompletenessService) {
        this.tutorRepository = tutorRepository;
        this.subjectRepository = subjectRepository;
        this.gradeRepository = gradeRepository;
        this.tutorSearchService = tutorSearchService;
        this.tutorCompletenessService = tutorCompletenessService;
    }

    // Resolves each slug via its repository and drops anything unrecognised - a slug that no
    // longer exists (an inactive subject/grade) should not blow up profile save, it should
    // just not count for search.
    private Set<Subject> resolveSubjects(List<String> slugs) {
        Set<Subject> resolved = new LinkedHashSet<>();
        if (slugs == null) return resolved;
        for (String slug : slugs) {
            subjectRepository.findBySlug(slug).ifPresent(resolved::add);
        }
        return resolved;
    }

    private Set<Grade> resolveGrades(List<String> slugs) {
        Set<Grade> resolved = new LinkedHashSet<>();
        if (slugs == null) return resolved;
        for (String slug : slugs) {
            gradeRepository.findBySlug(slug).ifPresent(resolved::add);
        }
        return resolved;
    }

    private boolean isAuthorized(String providedKey) {
        return adminKey != null && !adminKey.isBlank() && adminKey.equals(providedKey);
    }

    // GET /api/tutors?gradeSlug=class-5&subjectSlug=mathematics&mode=Online&type=Exam+Prep&lat=..&lng=..&radiusKm=5
    // Any param can be omitted to not filter on it. Only APPROVED listings are returned.
    //
    // Passing lat/lng ranks results by the published formula (see TutorSearchService) and
    // fills in distanceKm on each result; radiusKm additionally excludes anything farther
    // than that. Omitting lat/lng returns newest-first, unranked - used by callers that just
    // want a count or a full list, not a parent's personalised search.
    //
    // `area` is the pre-Phase-2 filter (exact match on the tutor's city-level area string).
    // Kept for callers that have not moved to locality/grade/subject search yet.
    @GetMapping
    public List<Tutor> search(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String gradeSlug,
            @RequestParam(required = false) String subjectSlug,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusKm) {
        TutorSearchService.Params params = new TutorSearchService.Params();
        params.area = area;
        params.mode = mode;
        params.type = type;
        params.gradeSlug = gradeSlug;
        params.subjectSlug = subjectSlug;
        params.lat = lat;
        params.lng = lng;
        params.radiusKm = radiusKm;
        return tutorSearchService.search(params);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tutor> getOne(@PathVariable Long id) {
        return tutorRepository.findById(id)
                .filter(t -> t.getStatus() == Tutor.Status.APPROVED)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/tutors - a tutor submits their listing. Goes in as PENDING until manually approved.
    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody TutorRequest req) {
        Tutor t = new Tutor();
        t.setName(req.getName());
        t.setPhone(req.getPhone());
        t.setArea(req.getArea());
        t.setFullAddress(req.getFullAddress());
        t.setLocality(req.getLocality());
        t.setLatitude(req.getLatitude());
        t.setLongitude(req.getLongitude());
        t.setModes(req.getModes());
        t.setTypes(req.getTypes());
        t.setGradeSubjects(req.getGradeSubjects());
        t.setPriceType(req.getPriceType());
        t.setPrice(req.getPrice());
        t.setPriceUnit(req.getPriceUnit());
        t.setBatchType(req.getBatchType());
        t.setTrialAvailable(req.getTrialAvailable());
        t.setPreferredTimings(req.getPreferredTimings());
        t.setQualification(req.getQualification());
        t.setLanguages(req.getLanguages());
        t.setBio(req.getBio());
        t.setPhotoUrl(req.getPhotoUrl());
        t.setVideoUrl(req.getVideoUrl());
        t.setYearsExperience(req.getYearsExperience());
        t.setSubjects(resolveSubjects(req.getSubjectSlugs()));
        t.setGrades(resolveGrades(req.getGradeSlugs()));
        t.setCompletenessScore(tutorCompletenessService.score(t));
        t.setStatus(Tutor.Status.PENDING);

        Tutor saved = tutorRepository.save(t);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "status", saved.getStatus().toString(),
                "message", "Listing submitted. It will go live once reviewed."
        ));
    }

    // --- Logged-in tutor's own profile (requires a valid JWT - see security/JwtAuthFilter) ---

    private Long currentTutorId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) return null;
        return (Long) auth.getPrincipal();
    }

    // Entity-level subjects/grades stay @JsonIgnore so the (potentially long) search list
    // response never triggers a lazy load per tutor. /me returns exactly one tutor, so
    // building an explicit view with the slugs resolved is cheap and lets dashboard.html
    // show which subjects/grades are already selected when a tutor edits their profile.
    private Map<String, Object> toMeView(Tutor t) {
        Map<String, Object> m = new java.util.LinkedHashMap<>(toAdminView(t));
        m.put("subjectSlugs", t.getSubjects().stream().map(Subject::getSlug).toList());
        m.put("gradeSlugs", t.getGrades().stream().map(Grade::getSlug).toList());
        m.put("verified", t.isVerified());
        return m;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        Long tutorId = currentTutorId();
        if (tutorId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return tutorRepository.findById(tutorId)
                .<ResponseEntity<?>>map(t -> ResponseEntity.ok(toMeView(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@Valid @RequestBody TutorRequest req) {
        Long tutorId = currentTutorId();
        if (tutorId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return tutorRepository.findById(tutorId).map(t -> {
            t.setName(req.getName());
            t.setPhone(req.getPhone());
            t.setArea(req.getArea());
            t.setFullAddress(req.getFullAddress());
            t.setLocality(req.getLocality());
            t.setLatitude(req.getLatitude());
            t.setLongitude(req.getLongitude());
            t.setModes(req.getModes());
            t.setTypes(req.getTypes());
            t.setGradeSubjects(req.getGradeSubjects());
            t.setPriceType(req.getPriceType());
            t.setPrice(req.getPrice());
            t.setPriceUnit(req.getPriceUnit());
            t.setBatchType(req.getBatchType());
            t.setTrialAvailable(req.getTrialAvailable());
            t.setPreferredTimings(req.getPreferredTimings());
            t.setQualification(req.getQualification());
            t.setLanguages(req.getLanguages());
            t.setBio(req.getBio());
            t.setPhotoUrl(req.getPhotoUrl());
            t.setVideoUrl(req.getVideoUrl());
            t.setYearsExperience(req.getYearsExperience());
            t.setSubjects(resolveSubjects(req.getSubjectSlugs()));
            t.setGrades(resolveGrades(req.getGradeSlugs()));
            t.setCompletenessScore(tutorCompletenessService.score(t));
            // First real save of a completed profile moves it from DRAFT into the review queue.
            // Edits after that go back to PENDING too, so admin re-checks anything a tutor changes.
            if (t.getStatus() == Tutor.Status.DRAFT || t.getStatus() == Tutor.Status.APPROVED) {
                t.setStatus(Tutor.Status.PENDING);
            }
            Tutor saved = tutorRepository.save(t);
            return ResponseEntity.ok(Map.of(
                    "id", saved.getId(),
                    "status", saved.getStatus().toString(),
                    "message", "Saved. Your listing will be reviewed before it's visible to parents."
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- Admin endpoints - require X-Admin-Key header matching the padhora.admin-key value ---

    private Map<String, Object> toAdminView(Tutor t) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("phone", t.getPhone());
        m.put("area", t.getArea());
        m.put("locality", t.getLocality());
        m.put("latitude", t.getLatitude());
        m.put("longitude", t.getLongitude());
        m.put("fullAddress", t.getFullAddress());
        m.put("modes", t.getModes());
        m.put("types", t.getTypes());
        m.put("gradeSubjects", t.getGradeSubjects());
        m.put("priceType", t.getPriceType());
        m.put("price", t.getPrice());
        m.put("priceUnit", t.getPriceUnit());
        m.put("batchType", t.getBatchType());
        m.put("trialAvailable", t.getTrialAvailable());
        m.put("preferredTimings", t.getPreferredTimings());
        m.put("qualification", t.getQualification());
        m.put("languages", t.getLanguages());
        m.put("bio", t.getBio());
        m.put("photoUrl", t.getPhotoUrl());
        m.put("videoUrl", t.getVideoUrl());
        m.put("yearsExperience", t.getYearsExperience());
        m.put("status", t.getStatus());
        m.put("createdAt", t.getCreatedAt());
        return m;
    }

    @GetMapping("/admin/pending")
    public ResponseEntity<?> pending(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        if (!isAuthorized(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(tutorRepository.findByStatus(Tutor.Status.PENDING).stream().map(this::toAdminView).toList());
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> all(@RequestHeader(value = "X-Admin-Key", required = false) String key) {
        if (!isAuthorized(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(tutorRepository.findAll().stream().map(this::toAdminView).toList());
    }

    @PatchMapping("/admin/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        if (!isAuthorized(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return tutorRepository.findById(id).map(t -> {
            t.setStatus(Tutor.Status.APPROVED);
            tutorRepository.save(t);
            return ResponseEntity.ok(Map.of("id", id, "status", "APPROVED"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/admin/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        if (!isAuthorized(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return tutorRepository.findById(id).map(t -> {
            t.setStatus(Tutor.Status.REJECTED);
            tutorRepository.save(t);
            return ResponseEntity.ok(Map.of("id", id, "status", "REJECTED"));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        if (!isAuthorized(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!tutorRepository.existsById(id)) return ResponseEntity.notFound().build();
        tutorRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("id", id, "deleted", true));
    }
}
