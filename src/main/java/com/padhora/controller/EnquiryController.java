package com.padhora.controller;

import com.padhora.dto.EnquiryRequest;
import com.padhora.model.Enquiry;
import com.padhora.model.Tutor;
import com.padhora.repository.EnquiryRepository;
import com.padhora.repository.TutorRepository;
import com.padhora.service.OtpService;
import com.padhora.util.PhoneUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/enquiries")
public class EnquiryController {

    private final EnquiryRepository enquiryRepository;
    private final TutorRepository tutorRepository;
    private final OtpService otpService;

    public EnquiryController(EnquiryRepository enquiryRepository, TutorRepository tutorRepository, OtpService otpService) {
        this.enquiryRepository = enquiryRepository;
        this.tutorRepository = tutorRepository;
        this.otpService = otpService;
    }

    // Same parent hitting the same tutor again within this window is treated as a
    // double-click/retry, not a second genuine enquiry - see submit().
    private static final Duration DUPLICATE_WINDOW = Duration.ofMinutes(2);

    // Enquiry lifecycle state machine: which statuses a tutor can move an enquiry TO,
    // from a given current status. Anything not listed here is rejected.
    private static final Map<Enquiry.Status, EnumSet<Enquiry.Status>> ALLOWED_TRANSITIONS = new EnumMap<>(Enquiry.Status.class);
    static {
        ALLOWED_TRANSITIONS.put(Enquiry.Status.NEW, EnumSet.of(Enquiry.Status.VIEWED, Enquiry.Status.ACCEPTED, Enquiry.Status.DECLINED));
        ALLOWED_TRANSITIONS.put(Enquiry.Status.VIEWED, EnumSet.of(Enquiry.Status.ACCEPTED, Enquiry.Status.DECLINED));
        ALLOWED_TRANSITIONS.put(Enquiry.Status.ACCEPTED, EnumSet.of(Enquiry.Status.CONNECTED, Enquiry.Status.DECLINED));
        ALLOWED_TRANSITIONS.put(Enquiry.Status.CONNECTED, EnumSet.of(Enquiry.Status.TUITION_STARTED));
        ALLOWED_TRANSITIONS.put(Enquiry.Status.TUITION_STARTED, EnumSet.of(Enquiry.Status.COMPLETED));
        // DECLINED, COMPLETED, EXPIRED are terminal - no further tutor-driven transitions out of them.
    }

    // ==========================================================
    // PARENT SIDE - no login. Identity = phone number (+ OTP for lookups).
    // ==========================================================

    public static class OtpRequest {
        public String phone;
        public String code;
    }

    // "Request Tutor" form submit. No OTP needed here - submitting is low-risk (worst case,
    // spam enquiries, which the duplicate-window check below also guards against), it's
    // re-reading someone else's enquiries later that needs proof of ownership.
    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody EnquiryRequest req) {
        Tutor tutor = tutorRepository.findById(req.getTutorId())
                .filter(t -> t.getStatus() == Tutor.Status.APPROVED)
                .orElse(null);
        if (tutor == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "That tutor listing isn't available."));
        }

        if (!PhoneUtil.isPlausible(req.getParentPhone())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please enter a valid phone number."));
        }
        String parentPhone = PhoneUtil.toE164(req.getParentPhone());

        // Duplicate guard: same parent + same tutor within the last couple of minutes -
        // return the existing request instead of creating a new one.
        var recent = enquiryRepository.findFirstByTutorIdAndParentPhoneAndCreatedAtAfterOrderByCreatedAtDesc(
                tutor.getId(), parentPhone, Instant.now().minus(DUPLICATE_WINDOW));
        if (recent.isPresent()) {
            Enquiry existing = recent.get();
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", existing.getId(),
                    "publicToken", existing.getPublicToken(),
                    "status", existing.getStatus().toString(),
                    "message", "Your request has been sent."
            ));
        }

        Enquiry e = new Enquiry();
        e.setTutorId(tutor.getId());
        e.setPublicToken(UUID.randomUUID().toString());
        e.setParentName(req.getParentName());
        e.setParentPhone(parentPhone);
        e.setClassName(req.getClassName());
        e.setSubject(req.getSubject());
        e.setMode(req.getMode());
        e.setLocality(req.getLocality());
        e.setPreferredTiming(req.getPreferredTiming());
        e.setBudget(req.getBudget());
        e.setMessage(req.getMessage());
        e.setStatus(Enquiry.Status.NEW);

        Enquiry saved = enquiryRepository.save(e);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", saved.getId(),
                "publicToken", saved.getPublicToken(),
                "status", saved.getStatus().toString(),
                "message", "Your request has been sent."
        ));
    }

    // Builds the parent-facing view of an enquiry, including the tutor's name (the Enquiry row
    // itself only stores tutorId - without this, "My Requests" can't say which tutor a request
    // is for, which is the whole point of the list when a parent has more than one kid/request).
    private Map<String, Object> toParentView(Enquiry e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("publicToken", e.getPublicToken());
        m.put("tutorId", e.getTutorId());
        m.put("tutorName", tutorRepository.findById(e.getTutorId()).map(Tutor::getName).orElse("Tutor"));
        m.put("className", e.getClassName());
        m.put("subject", e.getSubject());
        m.put("mode", e.getMode());
        m.put("locality", e.getLocality());
        m.put("preferredTiming", e.getPreferredTiming());
        m.put("budget", e.getBudget());
        m.put("status", e.getStatus().toString());
        m.put("createdAt", e.getCreatedAt());
        m.put("updatedAt", e.getUpdatedAt());
        return m;
    }

    // Success-screen deep link / status check for one specific request - the opaque token
    // stands in for auth here since it's only ever known to the parent who just submitted it.
    @GetMapping("/track/{token}")
    public ResponseEntity<?> track(@PathVariable String token) {
        return enquiryRepository.findByPublicToken(token)
                .<ResponseEntity<?>>map(e -> ResponseEntity.ok(toParentView(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Fired when the parent taps the secondary "WhatsApp Tutor" button after submitting.
    @PatchMapping("/track/{token}/whatsapp-click")
    public ResponseEntity<?> trackWhatsappClick(@PathVariable String token) {
        return enquiryRepository.findByPublicToken(token).map(e -> {
            e.setWhatsappClickedAt(Instant.now());
            enquiryRepository.save(e);
            return ResponseEntity.ok(Map.of("ok", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- "My Requests": phone + OTP, no account. See OtpService for the lockout/stub-mode details. ---

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody OtpRequest req) {
        if (!PhoneUtil.isPlausible(req.phone)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please enter a valid phone number."));
        }
        String e164 = PhoneUtil.toE164(req.phone);
        var outcome = otpService.request(e164);

        if (outcome.result == OtpService.RequestResult.THROTTLED) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "A code was just sent - please wait a bit before requesting another."));
        }

        var body = new LinkedHashMap<String, Object>();
        body.put("message", "We've sent a code to " + e164 + ".");
        if (otpService.isStubMode()) {
            // STUB MODE: no SMS provider connected yet, so the code is echoed here for testing.
            // Only active when PADHORA_OTP_STUB_MODE=true is explicitly set - see OtpService.
            body.put("devOtp", outcome.code);
            body.put("stubMode", true);
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpRequest req) {
        if (req.phone == null || req.code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone and code are required."));
        }
        String e164 = PhoneUtil.toE164(req.phone);
        var outcome = otpService.verify(e164, req.code.trim());

        if (outcome.result == OtpService.VerifyResult.LOCKED) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Too many incorrect attempts. Please try again in 15 minutes."));
        }
        if (outcome.result == OtpService.VerifyResult.INCORRECT) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Incorrect or expired code."));
        }
        return ResponseEntity.ok(Map.of("sessionToken", outcome.sessionToken));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(@RequestHeader(value = "X-Session-Token", required = false) String sessionToken) {
        String phone = otpService.resolveSession(sessionToken);
        if (phone == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<Map<String, Object>> result = enquiryRepository.findByParentPhoneOrderByCreatedAtDesc(phone)
                .stream().map(this::toParentView).toList();
        return ResponseEntity.ok(result);
    }

    // ==========================================================
    // TUTOR SIDE - reuses the same JWT pattern as TutorController.
    // ==========================================================

    private Long currentTutorId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) return null;
        return (Long) auth.getPrincipal();
    }

    // Tutor's inbox. Marks any NEW enquiries as VIEWED as a side effect of loading it -
    // that's the simplest honest definition of "the tutor has seen this".
    @GetMapping("/tutor/inbox")
    public ResponseEntity<?> inbox() {
        Long tutorId = currentTutorId();
        if (tutorId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Enquiry> enquiries = enquiryRepository.findByTutorIdOrderByCreatedAtDesc(tutorId);
        enquiries.stream()
                .filter(e -> e.getStatus() == Enquiry.Status.NEW)
                .forEach(e -> e.setStatus(Enquiry.Status.VIEWED));
        enquiryRepository.saveAll(enquiries);

        return ResponseEntity.ok(enquiries);
    }

    public static class StatusUpdateRequest {
        public String status; // ACCEPTED | DECLINED | CONNECTED | TUITION_STARTED | COMPLETED
    }

    @PatchMapping("/tutor/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest req) {
        Long tutorId = currentTutorId();
        if (tutorId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Enquiry.Status newStatus;
        try {
            newStatus = Enquiry.Status.valueOf(req.status);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Unknown status: " + req.status));
        }

        var opt = enquiryRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();

        Enquiry e = opt.get();
        if (!e.getTutorId().equals(tutorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not your enquiry."));
        }

        EnumSet<Enquiry.Status> allowed = ALLOWED_TRANSITIONS.getOrDefault(e.getStatus(), EnumSet.noneOf(Enquiry.Status.class));
        if (!allowed.contains(newStatus)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Can't move a " + e.getStatus() + " request to " + newStatus + "."));
        }

        e.setStatus(newStatus);
        enquiryRepository.save(e);
        return ResponseEntity.ok(Map.of("id", e.getId(), "status", e.getStatus().toString()));
    }
}
