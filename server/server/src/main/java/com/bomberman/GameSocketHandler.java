package com.bomberman;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class GameSocketHandler extends TextWebSocketHandler {

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("🔗 New player connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("📨 Received: " + payload);

        String response = switch (payload) {
            case "MOVE UP" -> "🡹 Player moved up!";
            case "MOVE DOWN" -> "🡻 Player moved down!";
            case "MOVE LEFT" -> "🡸 Player moved left!";
            case "MOVE RIGHT" -> "🡺 Player moved right!";
            default -> "🤔 Unknown action!";
        };

        session.sendMessage(new TextMessage(response));
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        System.out.println("❌ Player disconnected: " + session.getId());
    }
}
