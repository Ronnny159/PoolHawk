package com.poolhawk.api.websocket;

import com.poolhawk.api.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * Interceptor para autenticar conexiones WebSocket con JWT.
 * Extrae el token del query parameter (?token=...) o del header Authorization.
 * Valida el token y extrae el matchId para aislamiento de sesiones.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private final JwtService jwtService;

    public JwtHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

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
            List<String> authHeader = request.getHeaders().get("Authorization");
            if (authHeader != null && !authHeader.isEmpty()) {
                String header = authHeader.get(0);
                if (header.startsWith("Bearer ")) {
                    token = header.substring(7);
                }
            }
        }

        // 3. Validar token si está presente
        if (token != null && !token.isBlank()) {
            try {
                if (jwtService.validateToken(token)) {
                    String username = jwtService.getUsernameFromToken(token);
                    // Extraer matchId del token si está presente (claim personalizado)
                    String matchId = jwtService.getMatchIdFromToken(token);
                    
                    attributes.put("jwt_token", token);
                    attributes.put("username", username);
                    if (matchId != null) {
                        attributes.put("matchId", matchId);
                        log.info("WebSocket handshake OK - user: {}, matchId: {}", username, matchId);
                    } else {
                        log.info("WebSocket handshake OK - user: {} (sin matchId específico)", username);
                    }
                    return true;
                } else {
                    log.warn("WebSocket handshake RECHAZADO - Token JWT inválido o expirado");
                    response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    return false;
                }
            } catch (Exception e) {
                log.warn("WebSocket handshake RECHAZADO - Error validando token: {}", e.getMessage());
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }
        } else {
            log.warn("WebSocket handshake RECHAZADO - Token nulo o vacío");
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // No necesitamos lógica post-handshake
    }
}