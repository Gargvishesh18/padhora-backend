package com.padhora.controller;

import com.padhora.dto.TutorRequest;
import com.padhora.model.Tutor;
import com.padhora.repository.TutorRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tutors")
public class TutorController {

    private final TutorRepository tutorRepository;

    @Value("${padhora.admin-key}")
    private String adminKey;

    public TutorController(TutorRepository tutorRepository) {
        this.tutorRepository = tutorRepository;
    }

    private boolean isAuthorized(String providedKey) {
        return adminKey != null && !adminKey.isBlank() && adminKey.equals(providedKey);
    }

    // GET /api/tutors?area=Mohali&mode=Online&type=Exam+Prep
    // Any param can be omitted to not filter on it. Only APPROVED listings are returned.
    @GetMapping
    public List<Tutor> search(
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String mode,
            @RequestParam(required = false) String type) {
        return tutorRepository.search(area, mode, type);
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
        t.setStatus(Tutor.Status.PENDING);

        Tutor saved = tutorRepository.save(t);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "status", saved.getStatus().toString(),
                "message", "Listing submitted. It will go live once reviewed."
        ));
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
