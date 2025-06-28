package com.example.mychess;

import java.io.*;
import java.net.*;
import java.util.*;

public class SocketClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final List<MessageListener> listeners = new ArrayList<>();

    public SocketClient(String host, int port, String username, MessageListener initialListener) throws IOException {
        if (initialListener != null) {
            listeners.add(initialListener);
        }

        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

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

    public void addListener(MessageListener listener) {
        listeners.add(listener);
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("CHALLENGE_FROM:")) {
            String challenger = message.substring("CHALLENGE_FROM:".length());
            for (MessageListener listener : listeners) {
                listener.onChallengeReceived(challenger);
            }

        } else if (message.startsWith("CHALLENGE_RESULT:")) {
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                String fromUser = parts[1];
                String response = parts[2];
                for (MessageListener listener : listeners) {
                    listener.onChallengeResponse(fromUser, response);
                }
            }

        } else if (message.startsWith("MOVE_FROM:")) {
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                String fromUser = parts[1];
                String moveData = parts[2];
                for (MessageListener listener : listeners) {
                    listener.onMoveReceived(fromUser, moveData);
                }
            }

        }
        else if (message.startsWith("START_GAME:")) {
            String opponent = message.substring("START_GAME:".length());
            for (MessageListener listener : listeners) {
                listener.onStartGame(opponent); // ✅ Notify listeners
            }

        } else {
            System.out.println("[Client] Unknown message: " + message);
        }
    }

    public interface MessageListener {
        void onChallengeReceived(String fromUser);
        void onChallengeResponse(String fromUser, String response);
        void onMoveReceived(String fromUser, String moveData);
        void onStartGame(String opponentUsername);

    }
}
