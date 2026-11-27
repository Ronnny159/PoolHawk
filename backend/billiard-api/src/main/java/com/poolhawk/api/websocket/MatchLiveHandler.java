package com.poolhawk.api.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manejador de WebSockets para el marcador en vivo.
 * Mantiene un registro de las sesiones conectadas por partida (matchId)
 * y permite enviar actualizaciones solo a las sesiones de la partida correspondiente.
 */
@Component
public class MatchLiveHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MatchLiveHandler.class);

    // Sesiones indexadas por matchId -> set de sessionIds
    private final Map<String, ConcurrentHashMap<String, WebSocketSession>> sessionsByMatch = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Obtener matchId de los atributos de la sesión (establecido por JwtHandshakeInterceptor)
        String matchId = (String) session.getAttributes().get("matchId");
        String username = (String) session.getAttributes().get("username");
        
        if (matchId != null) {
            sessionsByMatch.computeIfAbsent(matchId, k -> new ConcurrentHashMap<>())
                    .put(session.getId(), session);
            log.info("🟢 WebSocket conectado: sessionId={}, user={}, matchId={}", 
                    session.getId(), username, matchId);
        } else {
            // Conexión sin matchId específico (ej. admin, lobby)
            sessionsByMatch.computeIfAbsent("__global__", k -> new ConcurrentHashMap<>())
                    .put(session.getId(), session);
            log.info("🟢 WebSocket conectado (global): sessionId={}, user={}", 
                    session.getId(), username);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // Remover de todas las listas de matchId
        sessionsByMatch.values().forEach(sessions -> sessions.remove(session.getId()));
        // Limpiar mapas vacíos
        sessionsByMatch.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        log.info("🔴 WebSocket desconectado: sessionId={}, status={}", session.getId(), status);
    }

    /**
     * Envía un mensaje JSON a todas las sesiones de una partida específica.
     * Si matchId es null, envía a todas las sesiones (broadcast global).
     */
    public void broadcastToMatch(String matchId, String jsonMessage) {
        TextMessage message = new TextMessage(jsonMessage);
        ConcurrentHashMap<String, WebSocketSession> sessions = sessionsByMatch.get(matchId);
        if (sessions != null) {
            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    log.warn("Error enviando mensaje WS a session {}: {}", session.getId(), e.getMessage());
                }
            });
        }
    }

    /**
     * Envía un mensaje JSON a todas las sesiones conectadas (broadcast global).
     */
    public void broadcast(String jsonMessage) {
        TextMessage message = new TextMessage(jsonMessage);
        sessionsByMatch.values().forEach(sessions -> 
            sessions.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    log.warn("Error enviando mensaje WS broadcast: {}", e.getMessage());
                }
            })
        );
    }

    /**
     * Obtiene el matchId desde el mensaje JSON para enrutar correctamente.
     */
    public String extractMatchIdFromMessage(String jsonMessage) {
        try {
            JsonNode node = objectMapper.readTree(jsonMessage);
            return node.has("id") ? node.get("id").asText() : null;
        } catch (Exception e) {
            log.warn("No se pudo extraer matchId del mensaje: {}", e.getMessage());
            return null;
        }
    }
}