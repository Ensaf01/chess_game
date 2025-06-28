package com.example.mychess;

import java.io.*;
import java.net.*;
public class SocketClient {
    private final PrintWriter out;
    private final BufferedReader in;
    MessageListener moveListener;
    //private boolean moveListener;

    public SocketClient(String host, int port, String username, MessageListener listener) throws IOException {
        this.moveListener = listener;
        Socket socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Sent login message after connect
        sendMessage("LOGIN:" + username);

        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    handleServerMessage(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("CHALLENGE_FROM:")) {
            String challenger = message.substring("CHALLENGE_FROM:".length());
            moveListener.onChallengeReceived(challenger);
        } else if (message.startsWith("CHALLENGE_RESULT:")) {
            // Format: CHALLENGE_RESULT:fromUser:ACCEPT/DECLINE
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                String fromUser = parts[1];
                String response = parts[2];
                moveListener.onChallengeResponse(fromUser, response);
            }
        } else if (message.startsWith("MOVE_FROM:")) {
            // Format: MOVE_FROM:fromUser:moveData
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                String fromUser = parts[1];
                String moveData = parts[2];
                if (this.moveListener != null) {
                    this.moveListener.accept(moveData); // new way for GameController to use it
                }
                moveListener.onMoveReceived(fromUser, moveData);
            }
        } else {
            System.out.println("[Client] Unknown message: " + message);
        }
    }
    public interface GameMoveListener {
        void onMove(String moveData);
    }


    private GameMoveListener gameMoveListener;
    public void setGameMoveListener(GameMoveListener moveListener) {
        this.gameMoveListener = moveListener;
    }
    private void receiveMove(String moveData) {
        if (gameMoveListener != null) {
            gameMoveListener.onMove(moveData);
        }
    }

    public interface MessageListener {
        void onChallengeReceived(String fromUser);
        void onChallengeResponse(String fromUser, String response);
        void onMoveReceived(String fromUser, String moveData);

        void accept(String moveData);
    }
}