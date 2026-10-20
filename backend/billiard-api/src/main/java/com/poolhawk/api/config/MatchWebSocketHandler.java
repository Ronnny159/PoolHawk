package com.poolhawk.api.config;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class MatchWebSocketHandler extends TextWebSocketHandler {

    // Guardamos todas las conexiones activas
    private final List<WebSocketSession> sesiones = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sesiones.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sesiones.remove(session);
    }

    // Método para emitir mensajes a todos los clientes conectados
    public void notificarActualizacion(String mensajeJson) {
        for (WebSocketSession sesion : sesiones) {
            try {
                if (sesion.isOpen()) {
                    sesion.sendMessage(new TextMessage(mensajeJson));
                }
            } catch (IOException e) {
                System.err.println("Error enviando WebSocket: " + e.getMessage());
            }
        }
    }
}