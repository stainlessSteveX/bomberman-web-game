package com.bomberman;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Timer;
import java.util.TimerTask;

public class GameSocketHandler extends TextWebSocketHandler {

    private final Map<WebSocketSession, Player> players = new ConcurrentHashMap<>();
    private final Timer gameLoopTimer = new Timer(true);

    private void broadcast(String event, Player player) {
        String message = "{ \"event\": \"" + event + "\", \"id\": \"" + player.getId() + "\", \"x\": " + player.getX() + ", \"y\": " + player.getY() + " }";

        for (WebSocketSession s : players.keySet()) {
            if (s.isOpen()) {
                try {
                    s.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    System.err.println("⚠️ Error sending WebSocket message: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (!players.containsKey(session)) { // 🛠️ Ensure the player isn’t recreated!
            Player player = new Player();
            players.put(session, player);
            System.out.println("🔗 New player joined: " + player.getId() + " at (" + player.getX() + ", " + player.getY() + ")");
            broadcast("NEW_PLAYER", player);
        }
    }

    /*
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Player player = new Player();
        players.put(session, player);

        System.out.println("🔗 New player joined: " + player.getId() + " at (" + player.getX() + ", " + player.getY() + ")");

        // Broadcast new player to everyone
        broadcast("NEW_PLAYER", player);
    }
    */

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("📨 Received: " + payload);

        Player player = players.get(session);

        if (player != null) {
            System.out.println("🔍 Before move: Player " + player.getId() + " at (" + player.getX() + ", " + player.getY() + ")");

            player.move(payload);

            System.out.println("✅ After move: Player " + player.getId() + " now at (" + player.getX() + ", " + player.getY() + ")");

            broadcast("MOVE", player);
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Player player = players.remove(session);
        if (player != null) {
            System.out.println("❌ Player disconnected: " + player.getId());

            // Broadcast player removal to everyone
            broadcast("REMOVE_PLAYER", player);
        }
    }

}
