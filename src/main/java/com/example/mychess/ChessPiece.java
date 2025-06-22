package com.example.mychess;

public class ChessPiece {

    public enum Type {
        KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
    }

    public enum Color {
        WHITE, BLACK
    }

    private Type type;
    private Color color;

    public ChessPiece(Type type, Color color) {
        this.type = type;
        this.color = color;
    }

    public Type getType() {
        return type;
    }
    public Color getColor() {
        return color;
    }

    public String getSymbol() {
        switch (type) {
            case KING:
                return color == Color.WHITE ? "♔" : "♚";
            case QUEEN:
                return color == Color.WHITE ? "♕" : "♛";
            case ROOK:
                return color == Color.WHITE ? "♖" : "♜";
            case BISHOP:
                return color == Color.WHITE ? "♗" : "♝";
            case KNIGHT:
                return color == Color.WHITE ? "♘" : "♞";
            case PAWN:
                return color == Color.WHITE ? "♙" : "♟";
            default:
                return "";
        }
    }

}
