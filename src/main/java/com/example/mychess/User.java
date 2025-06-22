package com.example.mychess;

public class User {
    private final int id;
    private final String username;
    private final int wins;
    private final int losses;

    public User(int id, String username, int wins, int losses) {
        this.id = id;
        this.username = username;
        this.wins = wins;
        this.losses = losses;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public int getWins() { return wins; }
    public int getLosses() { return losses; }
}
