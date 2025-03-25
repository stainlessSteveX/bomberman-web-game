package com.bomberman;

import java.util.UUID;

public class Player {
    private final String id;
    private int x;
    private int y;


    public Player() {
        this(0, 0); // Default to (0,0)
    }

    public Player(int x, int y) {
        this.id = UUID.randomUUID().toString();
        this.x = x;
        this.y = y;
    }


    public String getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void move(String direction, int[][] mapLayout) {
        int newX = x;
        int newY = y;

        switch (direction) {
            case "MOVE UP" -> newY--;
            case "MOVE DOWN" -> newY++;
            case "MOVE LEFT" -> newX--;
            case "MOVE RIGHT" -> newX++;
        }

        // Prevent out-of-bounds and wall collisions
        if (newX >= 0 && newX < mapLayout[0].length &&
                newY >= 0 && newY < mapLayout.length &&
                mapLayout[newY][newX] == 0) {
            this.x = newX;
            this.y = newY;
        }
    }

}
