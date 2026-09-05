package com.padhora.controller;

import com.padhora.model.Tutor;
import com.padhora.repository.TutorRepository;
import com.padhora.security.JwtUtil;
import com.padhora.service.OtpService;
import com.padhora.util.PhoneUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TutorRepository tutorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    public AuthController(TutorRepository tutorRepository, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, OtpService otpService) {
        this.tutorRepository = tutorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
    }

    public static class AuthRequest {
        @NotBlank @Email
        public String email;
        @NotBlank
        public String password;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody AuthRequest req) {
        if (req.email == null || req.password == null || req.password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and a password of at least 6 characters are required."));
        }
        String normalizedEmail = req.email.trim().toLowerCase();
        if (tutorRepository.findByEmail(normalizedEmail).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "An account with this email already exists. Try logging in instead."));
        }
        Tutor t = new Tutor();
        t.setEmail(normalizedEmail);
        t.setPasswordHash(passwordEncoder.encode(req.password));
        t.setStatus(Tutor.Status.DRAFT);
        Tutor saved = tutorRepository.save(t);

        String token = jwtUtil.generateToken(saved.getId(), saved.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("token", token, "tutorId", saved.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        if (req.email == null || req.password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required."));
        }
        String normalizedEmail = req.email.trim().toLowerCase();
        Optional<Tutor> found = tutorRepository.findByEmail(normalizedEmail);
        if (found.isEmpty() || found.get().getPasswordHash() == null
                || !passwordEncoder.matches(req.password, found.get().getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Incorrect email or password."));
        }
        Tutor t = found.get();
        String token = jwtUtil.generateToken(t.getId(), t.getEmail());
        return ResponseEntity.ok(Map.of("token", token, "tutorId", t.getId()));
    }

    // ==========================================================
    // PHONE + OTP (Phase 4) - the default path going forward. Existing email/password
    // accounts above are left exactly as they are: no migration, no forced switch. There is
    // no real tutor supply yet (checked before building this), so there is nothing to
    // migrate - this only matters if that ever changes.
    //
    // Reuses OtpService, the same one EnquiryController uses for parent "My Requests" - one
    // OTP implementation for the whole product, not two that can drift apart.
    // ==========================================================

    public static class OtpRequest {
        public String phone;
        public String code;
    }

    @PostMapping("/phone/request-otp")
    public ResponseEntity<?> requestPhoneOtp(@RequestBody OtpRequest req) {
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
            // STUB MODE ONLY - see OtpService. Not a real SMS send yet; PADHORA_OTP_STUB_MODE
            // must stay explicitly true until a provider (MSG91/Fast2SMS) is wired in, and
            // this field must never appear once real tutors are signing up for real.
            body.put("devOtp", outcome.code);
            body.put("stubMode", true);
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping("/phone/verify-otp")
    public ResponseEntity<?> verifyPhoneOtp(@RequestBody OtpRequest req) {
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

        // Find-or-create: a verified phone IS the account. Looked up via authPhone, a
        // dedicated identity column - NOT the free-text `phone` contact field, which real
        // tutors have already entered non-uniquely (see migration V10). First-ever
        // verification for a number creates a fresh DRAFT tutor, same starting state as
        // email signup, with `phone` prefilled too so the profile has a contact number by
        // default - a tutor can still edit it separately from their login number.
        Tutor t = tutorRepository.findByAuthPhone(e164).orElseGet(() -> {
            Tutor fresh = new Tutor();
            fresh.setAuthPhone(e164);
            fresh.setPhone(e164);
            fresh.setStatus(Tutor.Status.DRAFT);
            return tutorRepository.save(fresh);
        });

        String token = jwtUtil.generateToken(t.getId(), t.getEmail());
        boolean isNewTutor = t.getName() == null;
        return ResponseEntity.ok(Map.of("token", token, "tutorId", t.getId(), "isNewTutor", isNewTutor));
    }
}
