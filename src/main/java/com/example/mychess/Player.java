package com.example.mychess;

public class Player {
    private String username;
    private int wins;
    private int losses;
    private String category;

    public Player(String username, int wins, int losses, String category) {
        this.username = username;
        this.wins = wins;
        this.losses = losses;
        this.category = category;
    }

    public String getUsername() {
        return username;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public String getCategory() {
        return category;
    }
}
