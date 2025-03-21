package com.bomberman;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameSocketHandler extends TextWebSocketHandler {

    private final Map<WebSocketSession, Player> players = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Player player = new Player();
        players.put(session, player);
        System.out.println("🔗 New player connected: " + player.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("📨 Received: " + payload);

        Player player = players.get(session);
        if (player != null) {
            player.move(payload);
            String response = "📍 Player " + player.getId() + " moved to (" + player.getX() + ", " + player.getY() + ")";
            session.sendMessage(new TextMessage(response));
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Player player = players.remove(session);
        if (player != null) {
            System.out.println("❌ Player disconnected: " + player.getId());
        }
    }
}
