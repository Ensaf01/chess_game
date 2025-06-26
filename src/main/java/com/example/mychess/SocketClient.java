package com.example.mychess;

import java.io.*;
import java.net.*;

public class SocketClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    MessageListener listener;

    public SocketClient(String host, int port, String username, MessageListener listener) throws IOException {
        this.listener = listener;
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Send login message right after connection
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
        // Example message types:
        // CHALLENGE_FROM:alice
        // CHALLENGE_RESULT:bob:ACCEPT
        // MOVE_FROM:alice:6,4,4,4

        if (message.startsWith("CHALLENGE_FROM:")) {
            String challenger = message.substring("CHALLENGE_FROM:".length());
            listener.onChallengeReceived(challenger);
        } else if (message.startsWith("CHALLENGE_RESULT:")) {
            // Format: CHALLENGE_RESULT:fromUser:ACCEPT/DECLINE
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                String fromUser = parts[1];
                String response = parts[2];
                listener.onChallengeResponse(fromUser, response);
            }
        } else if (message.startsWith("MOVE_FROM:")) {
            // Format: MOVE_FROM:fromUser:moveData
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                String fromUser = parts[1];
                String moveData = parts[2];
                listener.onMoveReceived(fromUser, moveData);
            }
        } else {
            System.out.println("[Client] Unknown message: " + message);
        }
    }

    public interface MessageListener {
        void onChallengeReceived(String fromUser);
        void onChallengeResponse(String fromUser, String response);
        void onMoveReceived(String fromUser, String moveData);
    }
}
