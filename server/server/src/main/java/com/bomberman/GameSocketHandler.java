package com.bomberman;

import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.Timer;
import java.util.TimerTask;

import java.util.List;
import java.util.ArrayList;


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

    private void broadcastRaw(String message) {
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


    public GameSocketHandler() {
        // Start the game loop
        gameLoopTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                broadcastAllPlayerPositions();
            }
        }, 0, 33); // Runs every 33ms (~30 updates per second)
    }

    private void broadcastAllPlayerPositions() {
        for (Player player : players.values()) {
            broadcast("MOVE", player);
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
        Player player = players.get(session);

        if (player != null) {
            if ("DROP_BOMB".equals(payload)) {
                placeBomb(player.getX(), player.getY());
            } else {
                player.move(payload);
            }
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

    private void explodeBomb(int x, int y) {
        System.out.println("🔥 Bomb exploded at (" + x + ", " + y + ")");

        int range = 3;
        List<String> affectedPlayers = new ArrayList<>();

        for (WebSocketSession session : players.keySet()) {
            Player player = players.get(session);
            if (player == null) continue;

            // Check if player is in the explosion range
            if ((player.getX() == x && Math.abs(player.getY() - y) <= range) ||
                    (player.getY() == y && Math.abs(player.getX() - x) <= range)) {
                affectedPlayers.add("\"" + player.getId() + "\""); // ✅ Add quotes to player ID
            }
        }

        // Send explosion data
        String explosionMessage = "{ \"event\": \"EXPLOSION\", \"tiles\": [";

        for (int i = 1; i <= range; i++) {
            explosionMessage += "{ \"x\": " + (x + i) + ", \"y\": " + y + " }, ";
            explosionMessage += "{ \"x\": " + (x - i) + ", \"y\": " + y + " }, ";
            explosionMessage += "{ \"x\": " + x + ", \"y\": " + (y + i) + " }, ";
            explosionMessage += "{ \"x\": " + x + ", \"y\": " + (y - i) + " }, ";
        }

        explosionMessage = explosionMessage.substring(0, explosionMessage.length() - 2); // Remove last comma
        explosionMessage += "], \"playersHit\": " + affectedPlayers + " }";

        broadcastRaw(explosionMessage);

        // Remove affected players
        for (String playerId : affectedPlayers) {
            removePlayerById(playerId.replace("\"", "")); // ✅ Remove quotes when calling method
        }
    }


    private void removePlayerById(String playerId) {
        for (WebSocketSession session : players.keySet()) {
            Player player = players.get(session);
            if (player != null && player.getId().equals(playerId)) {
                players.remove(session);
                broadcastRaw("{ \"event\": \"REMOVE_PLAYER\", \"id\": \"" + playerId + "\" }");
                break;
            }
        }
    }



    private void placeBomb(int x, int y) {
        System.out.println("💣 Bomb placed at (" + x + ", " + y + ")");

        // Notify all players about the bomb
        broadcastRaw("{ \"event\": \"BOMB_PLACED\", \"x\": " + x + ", \"y\": " + y + " }");

        // Schedule explosion after 3 seconds
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                explodeBomb(x, y);
            }
        }, 3000);
    }


}
