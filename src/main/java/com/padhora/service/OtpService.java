package com.padhora.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// ============================================================================
// STUB OTP SERVICE - no real SMS provider wired up yet.
//
// request(phone) generates a code and logs it server-side (and, if
// padhora.otp.stub-mode=true, returns it in the API response) instead of
// sending a real SMS. This lets the full request -> verify -> "my requests"
// flow be built and tested end-to-end today, with zero SMS cost.
//
// TO GO LIVE: replace the body of request() that logs the code with a call to
// an SMS provider (Twilio / MSG91 / etc), and set padhora.otp.stub-mode=false
// so the code stops being echoed back in the response. Nothing else in this
// class, or in EnquiryController, or in the frontend needs to change.
//
// State is in-memory (ConcurrentHashMap), not persisted - fine for a single
// Railway instance; codes are short-lived anyway. If this ever runs on
// multiple instances, move this to a DB table or Redis.
// ============================================================================
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    @Value("${padhora.otp.stub-mode:true}")
    private boolean stubMode;

    private record OtpEntry(String code, Instant expiresAt) {}
    private record Session(String phone, Instant expiresAt) {}

    private final Map<String, OtpEntry> pendingCodes = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();

    /** Generates and "sends" an OTP for a phone number. Returns the code only in stub mode. */
    public String request(String e164Phone) {
        String code = String.format("%04d", random.nextInt(10_000));
        pendingCodes.put(e164Phone, new OtpEntry(code, Instant.now().plus(OTP_TTL)));

        // STUB: this is where a real SMS send call goes.
        log.info("[STUB SMS] OTP for {} is {} (expires in {} min)", e164Phone, code, OTP_TTL.toMinutes());

        return stubMode ? code : null;
    }

    public boolean isStubMode() { return stubMode; }

    /** Verifies a code and, on success, issues a session token for the phone-lookup endpoint. */
    public String verify(String e164Phone, String code) {
        OtpEntry entry = pendingCodes.get(e164Phone);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) return null;
        if (!entry.code().equals(code)) return null;

        pendingCodes.remove(e164Phone);
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(e164Phone, Instant.now().plus(SESSION_TTL)));
        return token;
    }

    /** Resolves a session token from the "my requests" screen back to a verified phone number. */
    public String resolveSession(String token) {
        if (token == null) return null;
        Session s = sessions.get(token);
        if (s == null || s.expiresAt().isBefore(Instant.now())) return null;
        return s.phone();
    }
}
