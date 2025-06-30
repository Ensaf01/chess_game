package com.example.mychess;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SocketServer {
    private static final int PORT = 5555;

    // Map username -> ClientHandler
    private static final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("[Server] Chess Socket Server started on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("[Server] New client connected: " + clientSocket);
            ClientHandler handler = new ClientHandler(clientSocket);
            handler.start();
        }
    }

    // Send message to a specific client by username
    public static void sendToUser(String username, String message) {
        ClientHandler client = clients.get(username);
        if (client != null) {
            client.sendMessage(message);
        } else {
            System.out.println("[Server] User '" + username + "' not connected.");
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println("[Server] Received: " + inputLine);

                    if (inputLine.startsWith("LOGIN:")) {
                        username = inputLine.substring(6);
                        clients.put(username, this);
                        System.out.println("[Server] User logged in: " + username);
                        continue;
                    }

                    String[] parts = inputLine.split(":", 4);
                    if (parts.length < 3) {
                        System.out.println("[Server] Invalid message format: " + inputLine);
                        continue;
                    }

                    String command = parts[0];
                    String fromUser = parts[1];
                    String toUser = parts[2];

                    switch (command) {
                        case "CHALLENGE":
                            ClientHandler opponentHandler  = clients.get(toUser);
                            if (opponentHandler  != null) {
                                sendToUser(toUser, "CHALLENGE_FROM:" + fromUser);
                                sendToUser(fromUser, "CHALLENGE_ACK:" + toUser);
                            } else {
                                sendToUser(fromUser, "PLAYER_NOT_AVAILABLE:" + toUser);
                            }
                            break;


                        case "CHALLENGE_RESPONSE":
                            String response = parts[3]; // ACCEPT or DECLINE
                            sendToUser(toUser, "CHALLENGE_RESULT:" + fromUser + ":" + response);

                            // ✅ If accepted, notify both players to open the game board
                            if ("ACCEPT".equalsIgnoreCase(response)) {
                                sendToUser(fromUser, "START_GAME:" + toUser); // Notify sender
                                sendToUser(toUser, "START_GAME:" + fromUser); // Notify receiver
                            }
                            break;

                        case "MOVE":
                            if (parts.length < 4) {
                                System.out.println("[Server] Invalid MOVE: " + inputLine);
                                break;
                            }
                            String moveData = parts[3]; // fromRow,fromCol,toRow,toCol
                            sendToUser(toUser, "MOVE_FROM:" + fromUser + ":" + moveData);
                            break;

                        default:
                            System.out.println("[Server] Unknown command: " + command);
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println("[Server] Client disconnected: " + socket);
            } finally {
                if (username != null) {
                    clients.remove(username);
                    System.out.println("[Server] User logged out: " + username);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
