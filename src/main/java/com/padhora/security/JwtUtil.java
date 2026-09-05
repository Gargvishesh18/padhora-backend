package com.padhora.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${padhora.jwt-secret}")
    private String secret;

    // Tokens last 90 days - tutors managing a listing shouldn't have to log in constantly.
    private static final long EXPIRATION_MS = 90L * 24 * 60 * 60 * 1000;

    private SecretKey key() {
        // The configured secret must be a base64-encoded string at least 32 bytes when decoded (HS256 requirement).
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // email is optional so phone-authenticated tutors (Phase 4) can get a token too - the
    // claim is simply omitted rather than stored as a literal "null" string.
    public String generateToken(Long tutorId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);
        var builder = Jwts.builder()
                .subject(String.valueOf(tutorId))
                .issuedAt(now)
                .expiration(expiry);
        if (email != null) builder.claim("email", email);
        return builder.signWith(key()).compact();
    }

    public Long validateAndGetTutorId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}
