package com.poolhawk.api.websocket;

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
 * Mantiene un registro de las sesiones conectadas y permite
 * enviar (broadcast) el estado actualizado de la partida.
 */
@Component
public class MatchLiveHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        System.out.println("🟢 Frontend conectado por WebSocket: " + session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        System.out.println("🔴 Frontend desconectado: " + session.getId());
    }

    /**
     * Envía un mensaje JSON a todos los navegadores conectados.
     */
    public void broadcast(String jsonMessage) {
        TextMessage message = new TextMessage(jsonMessage);
        sessions.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                System.err.println("Error enviando mensaje WS: " + e.getMessage());
            }
        });
    }
}