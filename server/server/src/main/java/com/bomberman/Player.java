package com.bomberman;

import java.util.UUID;

public class Player {
    private final String id;
    private int x;
    private int y;

    public Player() {
        this.id = UUID.randomUUID().toString();
        this.x = 0; // Default start position
        this.y = 0;
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

    public void move(String direction) {
        switch (direction) {
            case "MOVE UP" -> y -= 1;
            case "MOVE DOWN" -> y += 1;
            case "MOVE LEFT" -> x -= 1;
            case "MOVE RIGHT" -> x += 1;
        }
    }
}
