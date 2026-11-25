package com.poolhawk.api.controller;

import com.poolhawk.api.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        // TODO: Replace with UserDetailsService + database lookup in production
        // For MVP, use environment-configured credentials
        String configuredUser = System.getenv().getOrDefault("AUTH_USER", "arbitro");
        String configuredPasswordHash = System.getenv().getOrDefault("AUTH_PASSWORD_HASH",
            passwordEncoder.encode("admin123"));

        if (configuredUser.equals(username) && passwordEncoder.matches(password, configuredPasswordHash)) {
            String token = jwtService.generateToken(username);
            return ResponseEntity.ok(Map.of("token", token));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Credenciales invalidas"));
    }
}