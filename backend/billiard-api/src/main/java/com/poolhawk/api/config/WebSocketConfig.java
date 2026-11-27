package com.poolhawk.api.config;

import com.poolhawk.api.websocket.JwtHandshakeInterceptor;
import com.poolhawk.api.websocket.MatchLiveHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MatchLiveHandler matchLiveHandler;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(MatchLiveHandler matchLiveHandler, JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.matchLiveHandler = matchLiveHandler;
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(matchLiveHandler, "/ws/matches")
                .addInterceptors(jwtHandshakeInterceptor)
                // Permitir conexiones desde el frontend (Vite dev server, nginx, Docker)
                .setAllowedOrigins("http://localhost:5173", "http://localhost:80", "http://localhost", "https://tudominio.com")
                // Permitir conexiones desde cualquier origen en desarrollo (quitar en producción si se requiere)
                .setAllowedOriginPatterns("*");
    }
}
