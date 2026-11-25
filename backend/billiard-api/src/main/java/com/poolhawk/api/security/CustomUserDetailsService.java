package com.poolhawk.api.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    // BCrypt hash de "admin123"
    private static final String ARBITRO_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if ("arbitro".equals(username)) {
            return User.builder()
                    .username("arbitro")
                    .password(ARBITRO_PASSWORD_HASH)
                    .roles("ARBITRO")
                    .build();
        }
        throw new UsernameNotFoundException("Usuario no encontrado: " + username);
    }
}