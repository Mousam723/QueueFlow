package com.queueflow.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwsHeader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${queueflow.jwt.secret}") String secret,
            @Value("${queueflow.jwt.expiration-ms:3600000}") long expirationMillis
    ) {
        this.signingKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
        this.expirationMillis = expirationMillis;
    }

    public String generateToken(
            Long userId,
            String email,
            String role
    ) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(now.plusMillis(expirationMillis))
                )
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}