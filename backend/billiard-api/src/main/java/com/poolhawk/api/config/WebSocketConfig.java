package com.poolhawk.api.config;

import com.poolhawk.api.websocket.MatchLiveHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuración de WebSockets para exponer el endpoint /ws/matches.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MatchLiveHandler matchLiveHandler;

    public WebSocketConfig(MatchLiveHandler matchLiveHandler) {
        this.matchLiveHandler = matchLiveHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(matchLiveHandler, "/ws/matches")
                .setAllowedOrigins("*"); // Permite conexiones desde Vue (localhost:5173)
    }
}