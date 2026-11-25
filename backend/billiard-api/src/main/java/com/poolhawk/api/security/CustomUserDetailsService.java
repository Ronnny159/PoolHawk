package com.poolhawk.api.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> usuarios = new HashMap<>();

    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
        // Usuarios deben cargarse desde BD o script de inicialización.
        // No se hardcodean credenciales aquí.
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String hash = usuarios.get(username);
        if (hash == null) {
            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        }
        return User.builder()
                .username(username)
                .password(hash)
                .roles("ARBITRO")
                .build();
    }

    public void registrarUsuario(String username, String rawPassword) {
        usuarios.put(username, passwordEncoder.encode(rawPassword));
    }
}