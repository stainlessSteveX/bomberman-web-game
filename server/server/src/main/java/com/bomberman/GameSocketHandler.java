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

import java.util.Random;


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
            int[] spawn = findEmptySpawnPosition();
            Player player = new Player(spawn[0], spawn[1]);
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
                player.move(payload, mapLayout);
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

    private int isWall(int x, int y) {
        if (x < 0 || x >= mapLayout[0].length || y < 0 || y >= mapLayout.length) {
            return 1; // Treat out-of-bounds as solid wall
        }
        return mapLayout[y][x]; // 1 = solid, 2 = breakable, 0 = empty
    }

    private void explodeBomb(int x, int y) {
        System.out.println("🔥 Bomb exploded at (" + x + ", " + y + ")");

        int range = 3;
        List<String> affectedPlayers = new ArrayList<>();
        List<int[]> destroyedWalls = new ArrayList<>();
        List<String> explosionTiles = new ArrayList<>();

        // Always include the center tile
        explosionTiles.add("{ \"x\": " + x + ", \"y\": " + y + " }");

        for (int i = 1; i <= range; i++) {
            // Right
            if (isWall(x + i, y) == 1) break;
            if (isWall(x + i, y) == 2) { destroyedWalls.add(new int[]{x + i, y}); break; }
            explosionTiles.add("{ \"x\": " + (x + i) + ", \"y\": " + y + " }");
        }

        for (int i = 1; i <= range; i++) {
            // Left
            if (isWall(x - i, y) == 1) break;
            if (isWall(x - i, y) == 2) { destroyedWalls.add(new int[]{x - i, y}); break; }
            explosionTiles.add("{ \"x\": " + (x - i) + ", \"y\": " + y + " }");
        }

        for (int i = 1; i <= range; i++) {
            // Down
            if (isWall(x, y + i) == 1) break;
            if (isWall(x, y + i) == 2) { destroyedWalls.add(new int[]{x, y + i}); break; }
            explosionTiles.add("{ \"x\": " + x + ", \"y\": " + (y + i) + " }");
        }

        for (int i = 1; i <= range; i++) {
            // Up
            if (isWall(x, y - i) == 1) break;
            if (isWall(x, y - i) == 2) { destroyedWalls.add(new int[]{x, y - i}); break; }
            explosionTiles.add("{ \"x\": " + x + ", \"y\": " + (y - i) + " }");
        }

        // Detect hit players
        for (WebSocketSession session : players.keySet()) {
            Player player = players.get(session);
            if (player == null) continue;

            if ((player.getX() == x && Math.abs(player.getY() - y) <= range) ||
                    (player.getY() == y && Math.abs(player.getX() - x) <= range)) {
                affectedPlayers.add("\"" + player.getId() + "\"");
            }
        }

        // Build JSON
        StringBuilder json = new StringBuilder();
        json.append("{ \"event\": \"EXPLOSION\", ");
        json.append("\"tiles\": [").append(String.join(", ", explosionTiles)).append("], ");

        json.append("\"destroyedWalls\": [");
        for (int[] wall : destroyedWalls) {
            json.append("{ \"x\": ").append(wall[0]).append(", \"y\": ").append(wall[1]).append(" }, ");
        }
        if (!destroyedWalls.isEmpty()) {
            json.setLength(json.length() - 2); // remove last comma
        }
        json.append("], ");

        json.append("\"playersHit\": [").append(String.join(", ", affectedPlayers)).append("] }");

        broadcastRaw(json.toString());

        // Remove affected players
        for (String playerId : affectedPlayers) {
            removePlayerById(playerId.replace("\"", ""));
        }
    }


    // Function to check if a wall exists at given coordinates
    private int[][] mapLayout = {
            {1,1,1,1,1,1,1,1,1,1},
            {1,0,2,0,0,0,2,0,0,1},
            {1,0,1,0,1,1,0,1,0,1},
            {1,2,0,0,2,0,0,2,0,1},
            {1,0,1,2,1,1,2,1,0,1},
            {1,0,0,0,0,0,0,0,2,1},
            {1,1,2,1,1,1,2,1,1,1},
            {1,0,0,2,0,0,2,0,0,1},
            {1,0,1,0,1,1,0,1,0,1},
            {1,1,1,1,1,1,1,1,1,1}
    };

    private int[] findEmptySpawnPosition() {
        List<int[]> emptyTiles = new ArrayList<>();
        for (int y = 0; y < mapLayout.length; y++) {
            for (int x = 0; x < mapLayout[y].length; x++) {
                if (mapLayout[y][x] == 0) {
                    emptyTiles.add(new int[]{x, y});
                }
            }
        }
        if (!emptyTiles.isEmpty()) {
            return emptyTiles.get(new Random().nextInt(emptyTiles.size()));
        } else {
            return new int[]{1, 1}; // Fallback spawn
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
