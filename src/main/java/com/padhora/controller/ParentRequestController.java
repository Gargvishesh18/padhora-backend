package com.padhora.controller;

import com.padhora.dto.ParentRequestSubmission;
import com.padhora.model.Grade;
import com.padhora.model.ParentRequest;
import com.padhora.model.Subject;
import com.padhora.model.Tutor;
import com.padhora.repository.GradeRepository;
import com.padhora.repository.ParentRequestRepository;
import com.padhora.repository.SubjectRepository;
import com.padhora.repository.TutorRepository;
import com.padhora.util.PhoneUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Catch the parent when we have no tutor" - see BACKLOG.md and migration V9.
 *
 * A parent_request has no tutor at submission time; that's the point. Matching one to a
 * tutor is a manual admin action (matchTutor below), never automatic.
 */
@RestController
public class ParentRequestController {

    private final ParentRequestRepository parentRequestRepository;
    private final GradeRepository gradeRepository;
    private final SubjectRepository subjectRepository;
    private final TutorRepository tutorRepository;

    @Value("${padhora.admin-key}")
    private String adminKey;

    public ParentRequestController(ParentRequestRepository parentRequestRepository,
                                    GradeRepository gradeRepository,
                                    SubjectRepository subjectRepository,
                                    TutorRepository tutorRepository) {
        this.parentRequestRepository = parentRequestRepository;
        this.gradeRepository = gradeRepository;
        this.subjectRepository = subjectRepository;
        this.tutorRepository = tutorRepository;
    }

    private boolean isAuthorized(String providedKey) {
        return adminKey != null && !adminKey.isBlank() && adminKey.equals(providedKey);
    }

    // ==========================================================
    // PARENT SIDE - no login, no OTP. Submitting this is the lowest-friction thing on the
    // site by design (see BACKLOG.md): worst case is a spam row an admin ignores, not a
    // security problem, so there is nothing here to gate behind verification.
    // ==========================================================

    @PostMapping("/api/parent-requests")
    public ResponseEntity<?> submit(@Valid @RequestBody ParentRequestSubmission req) {
        if (!PhoneUtil.isPlausible(req.getPhone())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please enter a valid phone number."));
        }

        ParentRequest r = new ParentRequest();
        r.setParentPhone(PhoneUtil.toE164(req.getPhone()));
        r.setLocalityText(req.getLocality());
        r.setMode(req.getMode());
        r.setLatitude(req.getLatitude());
        r.setLongitude(req.getLongitude());

        // An unrecognised or omitted slug just means "open on this" - it does not block
        // submission. Same tolerance as TutorController's slug resolution.
        if (req.getGradeSlug() != null) {
            gradeRepository.findBySlug(req.getGradeSlug()).ifPresent(g -> r.setGradeId(g.getId()));
        }
        if (req.getSubjectSlug() != null) {
            subjectRepository.findBySlug(req.getSubjectSlug()).ifPresent(s -> r.setSubjectId(s.getId()));
        }

        ParentRequest saved = parentRequestRepository.save(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "message", "Got it - we'll find you a tutor and message you on WhatsApp."
        ));
    }

    // ==========================================================
    // ADMIN SIDE - X-Admin-Key header, same manual check as TutorController/ReferenceDataController.
    // ==========================================================

    private Map<String, Object> toAdminView(ParentRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("parentPhone", r.getParentPhone());
        m.put("localityText", r.getLocalityText());
        m.put("gradeName", r.getGradeId() == null ? null
                : gradeRepository.findById(r.getGradeId()).map(Grade::getName).orElse(null));
        m.put("subjectName", r.getSubjectId() == null ? null
                : subjectRepository.findById(r.getSubjectId()).map(Subject::getName).orElse(null));
        m.put("mode", r.getMode());
        m.put("latitude", r.getLatitude());
        m.put("longitude", r.getLongitude());
        m.put("status", r.getStatus().toString());
        m.put("matchedTutorId", r.getMatchedTutorId());
        m.put("matchedTutorName", r.getMatchedTutorId() == null ? null
                : tutorRepository.findById(r.getMatchedTutorId()).map(Tutor::getName).orElse(null));
        m.put("matchedAt", r.getMatchedAt());
        m.put("adminNotes", r.getAdminNotes());
        m.put("createdAt", r.getCreatedAt());
        m.put("updatedAt", r.getUpdatedAt());
        return m;
    }

    @GetMapping("/api/admin/parent-requests")
    public ResponseEntity<?> list(@RequestHeader(value = "X-Admin-Key", required = false) String key,
                                   @RequestParam(required = false) String status) {
        if (!isAuthorized(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<ParentRequest> requests;
        if (status != null && !status.isBlank()) {
            try {
                requests = parentRequestRepository.findTop300ByStatusOrderByCreatedAtDesc(
                        ParentRequest.Status.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(Map.of("error", "Unknown status: " + status));
            }
        } else {
            requests = parentRequestRepository.findTop300ByOrderByCreatedAtDesc();
        }
        return ResponseEntity.ok(requests.stream().map(this::toAdminView).toList());
    }

    public static class MatchRequest {
        public Long tutorId;
    }

    // Reassignable on purpose: the first match doesn't always pan out (tutor unreachable,
    // parent's need changes), and there is no automation second-guessing an admin's call here.
    @PatchMapping("/api/admin/parent-requests/{id}/match")
    public ResponseEntity<?> matchTutor(@PathVariable Long id, @RequestBody MatchRequest req,
                                         @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        if (!isAuthorized(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (req.tutorId == null) return ResponseEntity.badRequest().body(Map.of("error", "tutorId is required."));
        if (!tutorRepository.existsById(req.tutorId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "No such tutor."));
        }

        return parentRequestRepository.findById(id).map(r -> {
            r.setMatchedTutorId(req.tutorId);
            r.setMatchedAt(Instant.now());
            r.setStatus(ParentRequest.Status.MATCHED);
            parentRequestRepository.save(r);
            return ResponseEntity.ok(toAdminView(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/api/admin/parent-requests/{id}/close")
    public ResponseEntity<?> close(@PathVariable Long id,
                                    @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        if (!isAuthorized(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return parentRequestRepository.findById(id).map(r -> {
            r.setStatus(ParentRequest.Status.CLOSED);
            parentRequestRepository.save(r);
            return ResponseEntity.ok(toAdminView(r));
        }).orElse(ResponseEntity.notFound().build());
    }

    public static class NotesRequest {
        public String notes;
    }

    @PatchMapping("/api/admin/parent-requests/{id}/notes")
    public ResponseEntity<?> setNotes(@PathVariable Long id, @RequestBody NotesRequest req,
                                       @RequestHeader(value = "X-Admin-Key", required = false) String key) {
        if (!isAuthorized(key)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return parentRequestRepository.findById(id).map(r -> {
            r.setAdminNotes(req.notes);
            parentRequestRepository.save(r);
            return ResponseEntity.ok(toAdminView(r));
        }).orElse(ResponseEntity.notFound().build());
    }
}
