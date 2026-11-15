package com.poolhawk.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username) // En 0.12.x se usa .subject() en vez de .setSubject()
                .issuedAt(new Date()) // .issuedAt() en vez de .setIssuedAt()
                .expiration(new Date(System.currentTimeMillis() + expiration)) // .expiration() en vez de .setExpiration()
                .signWith(getSigningKey()) // La firma usa por defecto HS256 según el tamaño de la llave
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser() // En 0.12.x se usa parser() en vez de parserBuilder()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}