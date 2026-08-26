package com.padhora.controller;

import com.padhora.model.Tutor;
import com.padhora.repository.TutorRepository;
import com.padhora.security.JwtUtil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final TutorRepository tutorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(TutorRepository tutorRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.tutorRepository = tutorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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
}
