package com.poolhawk.api.config;

import com.poolhawk.api.entity.UsuarioEntity;
import com.poolhawk.api.repository.UsuarioRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class DataInitializer {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void inicializar() {
        String adminPassword = System.getenv("ADMIN_INITIAL_PASSWORD");
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Variable de entorno ADMIN_INITIAL_PASSWORD no configurada. " +
                    "Debe definir una contraseña inicial para el usuario admin en el archivo .env");
        }

        if (adminPassword.length() < 8) {
            throw new IllegalStateException(
                    "ADMIN_INITIAL_PASSWORD debe tener al menos 8 caracteres");
        }

        if (usuarioRepository.existsByUsername("admin")) {
            System.out.println("Usuario 'admin' ya existe, omitiendo creación inicial");
            return;
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
        System.out.println("Usuario 'admin' creado exitosamente");
    }
}