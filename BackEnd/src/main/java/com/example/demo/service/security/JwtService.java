package com.example.demo.service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    private final Key key;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes}") long expMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expMinutes * 60_000L;
    }

    public String generateToken(String subjectEmail, String rol) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(subjectEmail)
                .claim("rol", rol)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) key)
                .build()
                .parseSignedClaims(token);
    }

    public String getEmail(String token) {
        return parse(token).getPayload().getSubject();
    }

    public String getRol(String token) {
        Object r = parse(token).getPayload().get("rol");
        return r != null ? r.toString() : null;
    }
}
