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

import java.time.Instant;
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

    // ==========================================================
    // PARENT SIDE - no login. Identity = phone number (+ OTP for lookups).
    // ==========================================================

    public static class OtpRequest {
        public String phone;
        public String code;
    }

    // "Request Tutor" form submit. No OTP needed here - submitting is low-risk (worst case,
    // spam enquiries), it's re-reading someone else's enquiries later that needs proof of ownership.
    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody EnquiryRequest req) {
        Tutor tutor = tutorRepository.findById(req.getTutorId())
                .filter(t -> t.getStatus() == Tutor.Status.APPROVED)
                .orElse(null);
        if (tutor == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "That tutor listing isn't available."));
        }

        Enquiry e = new Enquiry();
        e.setTutorId(tutor.getId());
        e.setPublicToken(UUID.randomUUID().toString());
        e.setParentName(req.getParentName());
        e.setParentPhone(PhoneUtil.toE164(req.getParentPhone()));
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

    // Success-screen deep link / status check for one specific request - the opaque token
    // stands in for auth here since it's only ever known to the parent who just submitted it.
    @GetMapping("/track/{token}")
    public ResponseEntity<?> track(@PathVariable String token) {
        return enquiryRepository.findByPublicToken(token)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
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

    // --- "My Requests": phone + OTP, no account. See OtpService for the stub-mode explanation. ---

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody OtpRequest req) {
        if (req.phone == null || req.phone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required."));
        }
        String e164 = PhoneUtil.toE164(req.phone);
        String stubCode = otpService.request(e164);

        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("message", "We've sent a code to " + e164 + ".");
        if (otpService.isStubMode()) {
            // STUB MODE: no SMS provider connected yet, so the code is echoed here for testing.
            // Remove this once a real SMS provider is wired into OtpService.
            body.put("devOtp", stubCode);
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
        String sessionToken = otpService.verify(e164, req.code.trim());
        if (sessionToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Incorrect or expired code."));
        }
        return ResponseEntity.ok(Map.of("sessionToken", sessionToken));
    }

    @GetMapping("/mine")
    public ResponseEntity<?> mine(@RequestHeader(value = "X-Session-Token", required = false) String sessionToken) {
        String phone = otpService.resolveSession(sessionToken);
        if (phone == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(enquiryRepository.findByParentPhoneOrderByCreatedAtDesc(phone));
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
        e.setStatus(newStatus);
        enquiryRepository.save(e);
        return ResponseEntity.ok(Map.of("id", e.getId(), "status", e.getStatus().toString()));
    }
}
