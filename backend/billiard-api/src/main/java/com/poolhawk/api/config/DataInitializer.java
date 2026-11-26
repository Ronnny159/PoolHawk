package com.poolhawk.api.config;

import com.poolhawk.api.entity.UsuarioEntity;
import com.poolhawk.api.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.initial.password:}")
    private String adminInitialPassword;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void inicializar() {
        String adminPassword = adminInitialPassword;
        // Fallback to environment variable for production
        if ((adminPassword == null || adminPassword.isBlank()) && System.getenv("ADMIN_INITIAL_PASSWORD") != null) {
            adminPassword = System.getenv("ADMIN_INITIAL_PASSWORD");
        }

        // Si ya existe el admin, no necesitamos la contraseña
        if (usuarioRepository.existsByUsername("admin")) {
            log.info("Usuario 'admin' ya existe, omitiendo creación inicial");
            if (adminPassword == null || adminPassword.isBlank()) {
                log.warn("ADMIN_INITIAL_PASSWORD no configurada (no necesaria porque admin ya existe)");
            }
            return;
        }

        // Solo validamos la contraseña si vamos a crear el admin
        if (adminPassword == null || adminPassword.isBlank()) {
            log.error("ADMIN_INITIAL_PASSWORD no configurada. El usuario admin no existe y no se puede crear. " +
                    "Defina la variable en .env o application.yml para crear el admin inicial.");
            // No lanzamos excepción para no romper el arranque; el login fallará con 401 hasta que se configure
            return;
        }

        if (adminPassword.length() < 12) {
            log.warn("ADMIN_INITIAL_PASSWORD tiene menos de 12 caracteres. Se recomienda al menos 12 para seguridad.");
        }

        UsuarioEntity admin = new UsuarioEntity();
        admin.setId(UUID.randomUUID());
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRoles(List.of("ADMIN", "USER"));
        admin.setEnabled(true);
        admin.setCreatedAt(Instant.now());
        admin.setUpdatedAt(Instant.now());

        usuarioRepository.save(admin);
        log.info("Usuario 'admin' creado exitosamente");
    }
}