package com.poolhawk.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    private SecretKey signingKey;
    private boolean initialized = false;

    @PostConstruct
    public void initialize() {
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            log.error("JWT_SECRET no está configurado o es inseguro (mínimo 32 caracteres). " +
                    "El login fallará hasta que se configure correctamente en .env o application.yml");
            return;
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.initialized = true;
        log.info("JWT Service inicializado correctamente");
    }

    private SecretKey getSigningKey() {
        if (!initialized) {
            throw new IllegalStateException("JWT Service no inicializado: JWT_SECRET no configurado");
        }
        return signingKey;
    }

    public String generateToken(String username) {
        if (!initialized) {
            throw new IllegalStateException("JWT Service no inicializado: JWT_SECRET no configurado");
        }
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        if (!initialized) {
            return false;
        }
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        if (!initialized) {
            throw new IllegalStateException("JWT Service no inicializado: JWT_SECRET no configurado");
        }
        Claims claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
        return claims.getSubject();
    }
}