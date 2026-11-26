package com.poolhawk.api.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Interceptor para autenticar conexiones WebSocket con JWT.
 * Extrae el token de la query string o del header Authorization.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = null;

        // 1. Intentar obtener token de query parameter: ?token=xxx
        String query = request.getURI().getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "token".equals(kv[0])) {
                    token = kv[1];
                    break;
                }
            }
        }

        // 2. Si no, intentar obtener de header Authorization: Bearer xxx
        if (token == null) {
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        // 3. Si no hay token, permitir la conexión pero sin usuario autenticado
        // (El MatchLiveHandler puede filtrar mensajes por partida si es necesario)
        if (token != null) {
            attributes.put("jwt_token", token);
            log.info("WebSocket handshake con token JWT");
        } else {
            log.warn("WebSocket handshake SIN token JWT");
        }

        return true; // Permitir handshake
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No necesitamos lógica post-handshake
    }
}