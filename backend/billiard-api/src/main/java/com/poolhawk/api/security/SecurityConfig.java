package com.poolhawk.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración de seguridad para el entorno de demostración de PoolHawk.
 * Se desactiva la validación JWT y se permite el acceso a todos los endpoints
 * para garantizar el funcionamiento del WebSocket y la API durante la entrega.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Define la cadena de filtros de seguridad permitiendo todas las peticiones.
     * Se elimina la dependencia del filtro JWT para evitar bloqueos en el handshake del WebSocket.
     *
     * @param http Objeto de configuración de seguridad de Spring.
     * @return La cadena de filtros configurada.
     * @throws Exception Si ocurre un error en la configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // TODO ABIERTO PARA LA DEMO
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            // EL FILTRO JWT SE ELIMINÓ DE AQUÍ

        return http.build();
    }

    /**
     * Configura las reglas de CORS para permitir peticiones desde cualquier origen
     * (incluyendo el frontend en Vue y herramientas de prueba).
     *
     * @return La fuente de configuración de CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Usamos allowedOriginPatterns en lugar de allowedOrigins para permitir "*" con credenciales
        config.setAllowedOriginPatterns(List.of("*")); 
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}