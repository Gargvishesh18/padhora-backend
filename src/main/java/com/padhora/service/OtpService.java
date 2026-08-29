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
// request(phone) generates a code and logs it server-side (and, ONLY if
// padhora.otp.stub-mode=true is explicitly set, returns it in the API
// response) instead of sending a real SMS. This lets the full
// request -> verify -> "my requests" flow be tested end-to-end without an
// SMS provider connected.
//
// stub-mode defaults to FALSE (secure by default). Until a real SMS provider
// is wired in, set PADHORA_OTP_STUB_MODE=true in Railway to keep testing -
// otherwise nobody, including you, can retrieve a code to verify with.
//
// TO GO LIVE: replace the body of request() that logs the code with a call to
// an SMS provider (Twilio / MSG91 / etc), and remove/unset PADHORA_OTP_STUB_MODE
// so it falls back to the secure false default. Nothing else in this class,
// EnquiryController, or the frontend needs to change.
//
// State is in-memory (ConcurrentHashMap), not persisted - fine for a single
// Railway instance; codes/sessions are short-lived anyway, and a redeploy
// simply forces anyone mid-verification to request a fresh code. If this ever
// runs on multiple instances, move this to a DB table or Redis.
// ============================================================================
@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    // Brute-force protection: lock a phone out of verify attempts after this many
    // wrong codes, for this long. 5 tries against a 4-digit code (10,000 possibilities)
    // makes guessing impractical without needing a CAPTCHA at this stage.
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    // Request throttling: stop the same phone from triggering unlimited "sends"
    // (SMS cost/spam abuse once a real provider is wired in).
    private static final Duration MIN_GAP_BETWEEN_REQUESTS = Duration.ofSeconds(30);

    @Value("${padhora.otp.stub-mode:false}")
    private boolean stubMode;

    private record OtpEntry(String code, Instant expiresAt, Instant requestedAt) {}
    private record Session(String phone, Instant expiresAt) {}
    private record AttemptState(int failedAttempts, Instant lockedUntil) {}

    private final Map<String, OtpEntry> pendingCodes = new ConcurrentHashMap<>();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    private final SecureRandom random = new SecureRandom();

    public enum RequestResult { SENT, THROTTLED }

    public static class RequestOutcome {
        public final RequestResult result;
        public final String code; // only set when stubMode is on and result is SENT
        RequestOutcome(RequestResult result, String code) { this.result = result; this.code = code; }
    }

    /** Generates and "sends" an OTP for a phone number, unless a request came in too recently. */
    public RequestOutcome request(String e164Phone) {
        OtpEntry existing = pendingCodes.get(e164Phone);
        if (existing != null && existing.requestedAt().plus(MIN_GAP_BETWEEN_REQUESTS).isAfter(Instant.now())) {
            return new RequestOutcome(RequestResult.THROTTLED, null);
        }

        String code = String.format("%04d", random.nextInt(10_000));
        Instant now = Instant.now();
        pendingCodes.put(e164Phone, new OtpEntry(code, now.plus(OTP_TTL), now));

        // STUB: this is where a real SMS send call goes.
        log.info("[STUB SMS] OTP for {} is {} (expires in {} min)", e164Phone, code, OTP_TTL.toMinutes());

        return new RequestOutcome(RequestResult.SENT, stubMode ? code : null);
    }

    public boolean isStubMode() { return stubMode; }

    public enum VerifyResult { OK, LOCKED, INCORRECT }

    public static class VerifyOutcome {
        public final VerifyResult result;
        public final String sessionToken; // set only when result is OK
        VerifyOutcome(VerifyResult result, String sessionToken) { this.result = result; this.sessionToken = sessionToken; }
    }

    /** Verifies a code and, on success, issues a session token for the phone-lookup endpoint. */
    public VerifyOutcome verify(String e164Phone, String code) {
        AttemptState state = attempts.get(e164Phone);
        if (state != null && state.lockedUntil() != null && state.lockedUntil().isAfter(Instant.now())) {
            return new VerifyOutcome(VerifyResult.LOCKED, null);
        }

        OtpEntry entry = pendingCodes.get(e164Phone);
        boolean correct = entry != null && entry.expiresAt().isAfter(Instant.now()) && entry.code().equals(code);

        if (!correct) {
            int failed = (state == null ? 0 : state.failedAttempts()) + 1;
            Instant lockedUntil = failed >= MAX_VERIFY_ATTEMPTS ? Instant.now().plus(LOCKOUT_DURATION) : null;
            attempts.put(e164Phone, new AttemptState(failed, lockedUntil));
            return new VerifyOutcome(lockedUntil != null ? VerifyResult.LOCKED : VerifyResult.INCORRECT, null);
        }

        attempts.remove(e164Phone);
        pendingCodes.remove(e164Phone);
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(e164Phone, Instant.now().plus(SESSION_TTL)));
        return new VerifyOutcome(VerifyResult.OK, token);
    }

    /** Resolves a session token from the "my requests" screen back to a verified phone number. */
    public String resolveSession(String token) {
        if (token == null) return null;
        Session s = sessions.get(token);
        if (s == null || s.expiresAt().isBefore(Instant.now())) return null;
        return s.phone();
    }
}
