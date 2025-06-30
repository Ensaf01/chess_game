//gamecontroller

package com.example.mychess;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;


public class GameController {
    private String opponentUsername;
    private static final int INITIAL_TIME = 60;//(1min)
    @FXML
    private Label whitePlayerLabel;

    @FXML
    private Label blackPlayerLabel;

    @FXML
    private Label playerTurnLabel;
    @FXML
    private Label boardOwnerLabel;

    private int whiteTime = 60; // 1 minute
    private int blackTime = 60;

    private Thread whiteTimerThread;
    private Thread blackTimerThread;
    private ChessPiece.Color localPlayerColor;


    private volatile boolean whiteTimerRunning = false;
    private volatile boolean blackTimerRunning = false;
    private boolean gameEnded=false;
    private boolean isFlipped = false;

    private boolean sendMove=false;


    //
    //@FXML private Label playerTurnLabel;
    @FXML private GridPane chessBoard;

    private final int SIZE = 8;
    private StackPane[][] boardSquares = new StackPane[SIZE][SIZE];
    private ChessPiece[][] boardPieces = new ChessPiece[SIZE][SIZE];
    //private ChessPiece[][] board = new ChessPiece[8][8];


    private boolean isWhiteTurn = true;
    private int selectedRow = -1, selectedCol = -1;
    // initial setup code here
    @FXML
    public void initialize() {
        setupPieces();
        //updatePlayerTurn();
        drawBoard();
        resetGame();    // Place initial pieces & reset logic
        updateTimerLabel(whitePlayerLabel, whiteTime, "White");
        updateTimerLabel(blackPlayerLabel, blackTime, "Black");

        updatePlayerTurn();

    }



    // Black pieces here
    private void setupPieces() {
        // Set up white pieces
        boardPieces[7][0] = new ChessPiece(ChessPiece.Type.ROOK, ChessPiece.Color.WHITE);
        boardPieces[7][1] = new ChessPiece(ChessPiece.Type.KNIGHT, ChessPiece.Color.WHITE);
        boardPieces[7][2] = new ChessPiece(ChessPiece.Type.BISHOP, ChessPiece.Color.WHITE);
        boardPieces[7][3] = new ChessPiece(ChessPiece.Type.QUEEN, ChessPiece.Color.WHITE);
        boardPieces[7][4] = new ChessPiece(ChessPiece.Type.KING, ChessPiece.Color.WHITE);
        boardPieces[7][5] = new ChessPiece(ChessPiece.Type.BISHOP, ChessPiece.Color.WHITE);
        boardPieces[7][6] = new ChessPiece(ChessPiece.Type.KNIGHT, ChessPiece.Color.WHITE);
        boardPieces[7][7] = new ChessPiece(ChessPiece.Type.ROOK, ChessPiece.Color.WHITE);
        for (int col = 0; col < SIZE; col++) {
            boardPieces[6][col] = new ChessPiece(ChessPiece.Type.PAWN, ChessPiece.Color.WHITE);
        }

        // Set up black pieces
        boardPieces[0][0] = new ChessPiece(ChessPiece.Type.ROOK, ChessPiece.Color.BLACK);
        boardPieces[0][1] = new ChessPiece(ChessPiece.Type.KNIGHT, ChessPiece.Color.BLACK);
        boardPieces[0][2] = new ChessPiece(ChessPiece.Type.BISHOP, ChessPiece.Color.BLACK);
        boardPieces[0][3] = new ChessPiece(ChessPiece.Type.QUEEN, ChessPiece.Color.BLACK);
        boardPieces[0][4] = new ChessPiece(ChessPiece.Type.KING, ChessPiece.Color.BLACK);
        boardPieces[0][5] = new ChessPiece(ChessPiece.Type.BISHOP, ChessPiece.Color.BLACK);
        boardPieces[0][6] = new ChessPiece(ChessPiece.Type.KNIGHT, ChessPiece.Color.BLACK);
        boardPieces[0][7] = new ChessPiece(ChessPiece.Type.ROOK, ChessPiece.Color.BLACK);
        for (int col = 0; col < SIZE; col++) {
            boardPieces[1][col] = new ChessPiece(ChessPiece.Type.PAWN, ChessPiece.Color.BLACK);
        }
    }

    private void drawBoard() {
        chessBoard.getChildren().clear();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int displayRow = isFlipped ? SIZE - 1 - row : row;
                int displayCol = isFlipped ? SIZE - 1 - col : col;

                StackPane square = createSquare(row, col);  // row, col are still logic coords

                boardSquares[row][col] = square;

                chessBoard.add(square, displayCol, displayRow);
            }
        }
    }

    private StackPane createSquare(int row, int col) {
        Rectangle tile = new Rectangle(80, 80);
        tile.setFill((row + col) % 2 == 0 ? Color.BISQUE : Color.AQUA);

        Text pieceText = new Text();
        pieceText.setFont(Font.font(50));
        if (boardPieces[row][col] != null) {
            pieceText.setText(boardPieces[row][col].getSymbol());
        }

        StackPane square = new StackPane(tile, pieceText);

        // ✅ Adjust for flipped board view
        square.setOnMouseClicked(e -> {
            int realRow = isFlipped ? SIZE - 1 - row : row;
            int realCol = isFlipped ? SIZE - 1 - col : col;
            handleClick(realRow, realCol);
        });

        return square;
    }







    private void movePiece(int fromRow, int fromCol, int toRow, int toCol, boolean sendMove) {
        ChessPiece movingPiece = boardPieces[fromRow][fromCol];
        ChessPiece targetPiece = boardPieces[toRow][toCol];

        if (targetPiece != null && targetPiece.getColor() == movingPiece.getColor()) return;
        if (targetPiece != null && targetPiece.getType() == ChessPiece.Type.KING) return;

        boardPieces[toRow][toCol] = movingPiece;
        boardPieces[fromRow][fromCol] = null;

        // Switch timers
        if (isWhiteTurn) {
            stopWhiteTimer();
            startBlackTimer();
        } else {
            stopBlackTimer();
            startWhiteTimer();
        }

        isWhiteTurn = !isWhiteTurn;
        updatePlayerTurn();
        drawBoard();
        System.out.println("[GameController] Board updated after opponent move.");
        if (sendMove) {
            System.out.println("[GameController] Sending move: " + fromRow+","+fromCol+" → "+toRow+","+toCol);
            sendMoveToOpponent(fromRow, fromCol, toRow, toCol);
        }

        // Game over checks (optional)
        ChessPiece.Color currentPlayer = isWhiteTurn ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;
        if (isKingCaptured() || (isKingInCheck(currentPlayer) && !hasAnyValidMove(currentPlayer))) {
            if (gameEnded) return;
            gameEnded = true;
            chessBoard.setDisable(true);
            ChessPiece.Color winner = isWhiteTurn ? ChessPiece.Color.BLACK : ChessPiece.Color.WHITE;
            if (sendMove) {
                updatePlayerStats(winner); // ✅ Only trigger DB update once
            }
            //updatePlayerStats(winner);
            showWinDialog(winner);
        }
    }


    /*private void sendMoveToOpponent(int fromRow, int fromCol, int toRow, int toCol) {
        if (socketClient != null) {
            socketClient.sendMessage("MOVE:" + fromRow + "," + fromCol + "," + toRow + "," + toCol);
        }
    }*/



    private boolean isKingInCheck(ChessPiece.Color kingColor) {
        int kingRow = -1, kingCol = -1;

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                ChessPiece piece = boardPieces[row][col];
                if (piece != null && piece.getType() == ChessPiece.Type.KING && piece.getColor() == kingColor) {
                    kingRow = row;
                    kingCol = col;
                    break;
                }
            }
        }

        ChessPiece.Color opponentColor = (kingColor == ChessPiece.Color.WHITE) ? ChessPiece.Color.BLACK : ChessPiece.Color.WHITE;

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                ChessPiece piece = boardPieces[row][col];
                if (piece != null && piece.getColor() == opponentColor) {
                    List<int[]> validMoves = getValidMoves(piece, row, col);
                    for (int[] move : validMoves) {
                        if (move[0] == kingRow && move[1] == kingCol) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /*private List<int[]> getValidMoves(ChessPiece piece, int row, int col) {
    }*/
    private List<int[]> getValidMoves(ChessPiece piece, int row, int col) {
        List<int[]> moves = new ArrayList<>();
        if (piece == null) return moves;

        ChessPiece.Color color = piece.getColor();

        switch (piece.getType()) {
            case PAWN:
                int direction = (color == ChessPiece.Color.WHITE) ? -1 : 1;
                int startRow = (color == ChessPiece.Color.WHITE) ? 6 : 1;

                // Move forward
                if (isInBounds(row + direction, col) && boardPieces[row + direction][col] == null) {
                    moves.add(new int[]{row + direction, col});

                    // Move two squares from start
                    if (row == startRow && boardPieces[row + 2 * direction][col] == null) {
                        moves.add(new int[]{row + 2 * direction, col});
                    }
                }

                // Capture diagonally
                for (int dc = -1; dc <= 1; dc += 2) {
                    int newCol = col + dc;
                    int newRow = row + direction;
                    if (isInBounds(newRow, newCol)) {
                        ChessPiece target = boardPieces[newRow][newCol];
                        if (target != null && target.getColor() != color) {
                            moves.add(new int[]{newRow, newCol});
                        }
                    }
                }
                break;

            case KNIGHT:
                int[][] knightMoves = {{-2,-1}, {-2,1}, {-1,-2}, {-1,2}, {1,-2}, {1,2}, {2,-1}, {2,1}};
                for (int[] move : knightMoves) {
                    int r = row + move[0], c = col + move[1];
                    if (isInBounds(r, c) && (boardPieces[r][c] == null || boardPieces[r][c].getColor() != color)) {
                        moves.add(new int[]{r, c});
                    }
                }
                break;

            case BISHOP:
                addSlidingMoves(moves, row, col, color, new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}});
                break;

            case ROOK:
                addSlidingMoves(moves, row, col, color, new int[][]{{-1,0},{1,0},{0,-1},{0,1}});
                break;

            case QUEEN:
                addSlidingMoves(moves, row, col, color, new int[][]{{-1,0},{1,0},{0,-1},{0,1},{-1,-1},{-1,1},{1,-1},{1,1}});
                break;

            case KING:
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        if (dr == 0 && dc == 0) continue;
                        int r = row + dr, c = col + dc;
                        if (isInBounds(r, c) && (boardPieces[r][c] == null || boardPieces[r][c].getColor() != color)) {
                            moves.add(new int[]{r, c});
                        }
                    }
                }
                break;
        }

        return moves;
    }

    /* private void addSlidingMoves(List<int[]> moves, int row, int col, ChessPiece.Color color, int[][] ints) {
     }*/
    private boolean isInBounds(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    private void addSlidingMoves(List<int[]> moves, int row, int col, ChessPiece.Color color, int[][] directions) {
        for (int[] dir : directions) {
            int r = row + dir[0], c = col + dir[1];
            while (isInBounds(r, c)) {
                if (boardPieces[r][c] == null) {
                    moves.add(new int[]{r, c});
                } else {
                    if (boardPieces[r][c].getColor() != color) {
                        moves.add(new int[]{r, c});
                    }
                    break;
                }
                r += dir[0];
                c += dir[1];
            }
        }
    }



    private boolean isKingCaptured() {
        ChessPiece.Color opponentColor = isWhiteTurn ? ChessPiece.Color.BLACK : ChessPiece.Color.WHITE;

        // Search for the opponent's king on the board
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                ChessPiece piece = boardPieces[row][col];
                if (piece != null && piece.getType() == ChessPiece.Type.KING && piece.getColor() == opponentColor) {
                    return false; // King is still on the board
                }
            }
        }
        return true; // Opponent's king has been captured
    }

    private void resetGame() {
        gameEnded=false;
        // Clear the board
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                boardPieces[row][col] = null;

                // Clear UI again
                StackPane square = boardSquares[row][col];
                if (square != null) {
                    square.getChildren().removeIf(node -> node instanceof Label); // remove piece labels
                }
            }
        }

        isWhiteTurn = true;

        // Setup fresh board
        setupPieces();         // put pieces in initial position
        updatePlayerTurn();    // update turn label
        // int whiteTimeRemaining = 1 * 60;
        //int blackTimeRemaining = 1 * 60;
        stopWhiteTimer();
        stopBlackTimer();
        startWhiteTimer();
    }


    private void handleClick(int row, int col) {
        int actualRow = isFlipped ? SIZE - 1 - row : row;
        int actualCol = isFlipped ? SIZE - 1 - col : col;

        ChessPiece clicked = boardPieces[actualRow][actualCol];

        if ((isWhiteTurn && localPlayerColor != ChessPiece.Color.WHITE) ||
                (!isWhiteTurn && localPlayerColor != ChessPiece.Color.BLACK)) {
            return; // Not your turn
        }

        if (selectedRow == -1 && clicked != null && clicked.getColor() == (isWhiteTurn ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK)) {
            selectedRow = actualRow;
            selectedCol = actualCol;
            boardSquares[actualRow][actualCol].setStyle("-fx-border-color: red; -fx-border-width: 3;");
        } else if (selectedRow != -1) {
            if (isValidMove(selectedRow, selectedCol, actualRow, actualCol)) {
                movePiece(selectedRow, selectedCol, actualRow, actualCol, true);
            }
            boardSquares[selectedRow][selectedCol].setStyle("");
            selectedRow = -1;
            selectedCol = -1;
            drawBoard();
        }
    }




    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        ChessPiece piece = boardPieces[fromRow][fromCol];
        if (piece == null) return false;

        ChessPiece.Color pieceColor = piece.getColor();

        // Check for destination square
        ChessPiece targetPiece = boardPieces[toRow][toCol];

        // A piece cannot move to a square occupied by its own color
        if (targetPiece != null && targetPiece.getColor() == pieceColor) {
            return false; // Can't capture your own piece
        }
        if (targetPiece != null) {
            // Prevent capturing same color
            if (targetPiece.getColor() == pieceColor) {
                return false;
            }
            // Prevent capturing a King
            if (targetPiece.getType() == ChessPiece.Type.KING) {
                return false;
            }
        }


        // Pawn movement (simplified)

        int dir = pieceColor == ChessPiece.Color.WHITE ? -1 : 1;
        if (piece.getType() == ChessPiece.Type.PAWN) {
            if (fromCol == toCol && targetPiece == null) {
                // Regular forward move (1 square)
                return toRow == fromRow + dir ||
                        (fromRow == (pieceColor == ChessPiece.Color.WHITE ? 6 : 1) && toRow == fromRow + 2 * dir && boardPieces[fromRow + dir][toCol] == null);
            } else if (Math.abs(toCol - fromCol) == 1 && toRow == fromRow + dir && targetPiece != null && targetPiece.getColor() != pieceColor) {
                // Capture
                return true; // Capture move is valid
            }
        }


        // Rook movement logic

        if (piece.getType() == ChessPiece.Type.ROOK) {
            if (fromRow == toRow) {  // Moving horizontally
                for (int i = Math.min(fromCol, toCol) + 1; i < Math.max(fromCol, toCol); i++) {
                    if (boardPieces[fromRow][i] != null) return false;  // Path blocked
                }
                // Can capture
                if (targetPiece != null && targetPiece.getColor() != pieceColor) {
                    return true; // Capture
                }
                return true; // Valid move jodi target ghor empty thake
            } else if (fromCol == toCol) {  // Moving vertically
                for (int i = Math.min(fromRow, toRow) + 1; i < Math.max(fromRow, toRow); i++) {
                    if (boardPieces[i][fromCol] != null) return false;  // Path blocked
                }
                if (targetPiece != null && targetPiece.getColor() != pieceColor) {
                    return true; // Capture
                }
                return true; // Valid move
            }
        }

        // Knight movement logic (L-shape)
        if (piece.getType() == ChessPiece.Type.KNIGHT) {
            int rowDiff = Math.abs(fromRow - toRow);
            int colDiff = Math.abs(fromCol - toCol);
            if ((rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2)) {
                // Knight can move over pieces, but must capture opposite color
                if (targetPiece != null && targetPiece.getColor() != pieceColor) {
                    return true; // Capture
                }
                return true; // Valid move if target square is empty
            }
        }

        // Bishop movement logic
        if (piece.getType() == ChessPiece.Type.BISHOP) {
            if (Math.abs(fromRow - toRow) != Math.abs(fromCol - toCol)) return false;  // Must move diagonally
            int rowStep = fromRow < toRow ? 1 : -1;
            int colStep = fromCol < toCol ? 1 : -1;
            int row = fromRow + rowStep;
            int col = fromCol + colStep;

            while (row != toRow && col != toCol) {
                if (boardPieces[row][col] != null) return false;  // Path blocked
                row += rowStep;
                col += colStep;
            }

            if (targetPiece != null && targetPiece.getColor() != pieceColor) {
                return true; // Capture
            }
            return true; // Valid move if target square is empty
        }

        // Queen movement logic (rook + bishop)
        if (piece.getType() == ChessPiece.Type.QUEEN) {
            // Rook-like move (horizontal/vertical)
            if (fromRow == toRow) {
                for (int i = Math.min(fromCol, toCol) + 1; i < Math.max(fromCol, toCol); i++) {
                    if (boardPieces[fromRow][i] != null) return false;  // Path blocked
                }
                if (targetPiece != null && targetPiece.getColor() != pieceColor) {
                    return true; // Capture korle
                }
                return true; // Valid move if target square is empty
            } else if (fromCol == toCol) {
                for (int i = Math.min(fromRow, toRow) + 1; i < Math.max(fromRow, toRow); i++) {
                    if (boardPieces[i][fromCol] != null) return false;  // Path blocked
                }
                if (targetPiece != null && targetPiece.getColor() != pieceColor) {
                    return true; // Capture
                }
                return true; // Valid move if target square is empty
            }
            // Bishop-like move (diagonal)
            if (Math.abs(fromRow - toRow) == Math.abs(fromCol - toCol)) {
                int rowStep = fromRow < toRow ? 1 : -1;
                int colStep = fromCol < toCol ? 1 : -1;
                int row = fromRow + rowStep;
                int col = fromCol + colStep;

                while (row != toRow && col != toCol) {
                    if (boardPieces[row][col] != null) return false;  // Path blocked
                    row += rowStep;
                    col += colStep;
                }

                if (targetPiece != null && targetPiece.getColor() != pieceColor) {
                    return true; // Capture
                }
                return true; // Valid move if target square is empty
            }
        }

        // King movement logic
        if (piece.getType() == ChessPiece.Type.KING) {
            int rowDiff = Math.abs(fromRow - toRow);
            int colDiff = Math.abs(fromCol - toCol);
            if (rowDiff <= 1 && colDiff <= 1) {
                if (targetPiece != null && targetPiece.getColor() != pieceColor) {
                    return true; // Capture
                }
                return true; // Valid move if target square is empty
            }
        }
        // After successful move

        return false;
    }
    /*private boolean hasAnyValidMove(ChessPiece.Color color) {
        for (int fromRow = 0; fromRow < 8; fromRow++) {
            for (int fromCol = 0; fromCol < 8; fromCol++) {
                ChessPiece piece = boardPieces[fromRow][fromCol];
                if (piece != null && piece.getColor() == color) {
                    for (int toRow = 0; toRow < 8; toRow++) {
                        for (int toCol = 0; toCol < 8; toCol++) {
                            if (isValidMove(fromRow, fromCol, toRow, toCol)) {
                                return true; // At least one valid move exists
                            }
                        }
                    }
                }
            }
        }
        return false; // No valid moves found
    }*/
    private boolean hasAnyValidMove(ChessPiece.Color color) {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                ChessPiece piece = boardPieces[row][col];
                if (piece != null && piece.getColor() == color) {
                    List<int[]> moves = getValidMoves(piece, row, col);
                    for (int[] move : moves) {
                        // Simulate move
                        ChessPiece backup = boardPieces[move[0]][move[1]];
                        boardPieces[move[0]][move[1]] = piece;
                        boardPieces[row][col] = null;

                        boolean stillInCheck = isKingInCheck(color);

                        // Undo
                        boardPieces[row][col] = piece;
                        boardPieces[move[0]][move[1]] = backup;

                        if (!stillInCheck) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }





    /*private void showWinDialog(ChessPiece.Color winner) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Game Over");
    alert.setHeaderText(null);
    alert.setContentText(winner + " wins!");
    alert.showAndWait();
}*/
    private int whitePlayerId;
    private int blackPlayerId;
    public void setPlayerIds(int whiteId, int blackId) {
        this.whitePlayerId = whiteId;
        this.blackPlayerId = blackId;
    }

    private void showWinDialog(ChessPiece.Color winner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("Game Ended");
        alert.setContentText(winner == ChessPiece.Color.WHITE ? "White wins!" : "Black wins!");

        alert.getButtonTypes().setAll(ButtonType.OK);

        alert.showAndWait().ifPresent(response -> {
            closeGameWindow();  // ✅ Close only the game window
        });
    }

    private void closeGameWindow() {
        Stage stage = (Stage) chessBoard.getScene().getWindow();
        stage.close(); // close the game window

        if (dashboardController != null) {
            dashboardController.refreshDashboard(); // ✅ reload stats & player list
        }
    }




    private void sendPlayAgainRequest() {
        if (socketClient != null && opponentUsername != null) {
            socketClient.sendMessage("CHALLENGE:" + loggedInUsername + ":" + opponentUsername);
            System.out.println("[GameController] Sent play again request to " + opponentUsername);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Waiting for " + opponentUsername + " to accept rematch...");
            alert.show();
        }
    }
    private void goToDashboard() {
        if (dashboardController != null) {
            // Get current stage
            Stage stage = (Stage) chessBoard.getScene().getWindow();

            // Reuse the already-loaded dashboard scene
            Parent dashboardRoot = dashboardController.welcomeLabel.getScene().getRoot(); // Reuse scene
            stage.setScene(new Scene(dashboardRoot));
            stage.setTitle("Player Dashboard");
        } else {
            System.err.println("⚠ Dashboard controller is null. Cannot return.");
        }
    }



    private void updatePlayerStats(ChessPiece.Color winnerColor) {
        try (Connection conn = DBUtil.getConnection()){
            int winnerId = (winnerColor == ChessPiece.Color.WHITE) ? whitePlayerId : blackPlayerId;
            int loserId = (winnerColor == ChessPiece.Color.WHITE) ? blackPlayerId : whitePlayerId;

            // Don't update if winner or loser is AI or unset
            if (winnerId == -1 || loserId == -1) {
                System.out.println("Invalid player IDs, skipping update.");
                return;
            }

            // Update win
            try (PreparedStatement winStmt = conn.prepareStatement(
                    "UPDATE players SET wins = wins + 1 WHERE id = ?")) {
                winStmt.setInt(1, winnerId);
                winStmt.executeUpdate();
                System.out.println("Updated wins for player ID: " + winnerId);
            }

            // Update loss
            try (PreparedStatement loseStmt = conn.prepareStatement(
                    "UPDATE players SET losses = losses + 1 WHERE id = ?")) {
                loseStmt.setInt(1, loserId);
                loseStmt.executeUpdate();
                System.out.println("Updated losses for player ID: " + loserId);
            }

            // Insert match result into matches table
            try (PreparedStatement insertMatch = conn.prepareStatement(
                    "INSERT INTO matches (player1_id, player2_id, winner_id, match_date) VALUES (?, ?, ?, NOW())")) {
                insertMatch.setInt(1, whitePlayerId);
                insertMatch.setInt(2, blackPlayerId);
                insertMatch.setInt(3, winnerId);
                insertMatch.executeUpdate();
                System.out.println("Match inserted into 'matches' table.");
            }

            // Update player category & rankings
            updatePlayerCategory(conn, winnerId);
            updatePlayerCategory(conn, loserId);
            updatePlayerRankTitles();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Failed to update player stats.");
        }
    }


    private void updatePlayerRankTitles() {
        try (Connection conn = DBUtil.getConnection()) {
            String sql = "SELECT id FROM players ORDER BY wins DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                int rank = 1;

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String title;

                    if (rank <= 3) {
                        title = "Pro-Gamer";
                    } else if (rank <= 5) {
                        title = "Advanced";
                    } else {
                        title = "Beginner";
                    }

                    try (PreparedStatement update = conn.prepareStatement("UPDATE players SET rank_title = ? WHERE id = ?")) {
                        update.setString(1, title);
                        update.setInt(2, id);
                        update.executeUpdate();
                    }

                    rank++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void updatePlayerCategory(Connection conn, int playerId) throws Exception {
        String category;
        try (PreparedStatement stmt = conn.prepareStatement("SELECT wins FROM players WHERE id = ?")) {
            stmt.setInt(1, playerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int wins = rs.getInt("wins");

                if (wins >= 10) category = "A";
                else if (wins >= 5) category = "B";
                else category = "C";

                try (PreparedStatement update = conn.prepareStatement("UPDATE players SET category = ? WHERE id = ?")) {
                    update.setString(1, category);
                    update.setInt(2, playerId);
                    update.executeUpdate();
                }
            }
        }
    }



//thread



    private void updatePlayerTurn() {
        if (playerTurnLabel != null) {
            String currentPlayerName = isWhiteTurn ? whitePlayer : blackPlayer;
            playerTurnLabel.setText((isWhiteTurn ? "White" : "Black") + "'s Turn (" + currentPlayerName + ")");

            if (isWhiteTurn) {
                stopBlackTimer();
                blackTime = INITIAL_TIME;
                whiteTime = INITIAL_TIME;
                startWhiteTimer();
            } else {
                stopWhiteTimer();
                whiteTime = INITIAL_TIME;
                blackTime = INITIAL_TIME;
                startBlackTimer();
            }

            updateTimerLabel(whitePlayerLabel, whiteTime, "White");
            updateTimerLabel(blackPlayerLabel, blackTime, "Black");
        }
    }




    private void updateTimerLabel(Label label, int timeLeft, String playerName) {
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);
        Platform.runLater(() -> label.setText(playerName + ": " + timeStr));
    }

    private void startWhiteTimer() {
        stopWhiteTimer();  // Stop previous if running
        final int[] whiteTimeRemaining = {1 * 60}; // example: 5 minutes

        whiteTimerThread = new Thread(() -> {
            while (whiteTimeRemaining[0] > 0 && isWhiteTurn) {
                updateTimerLabel(whitePlayerLabel, whiteTimeRemaining[0], "White");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return; // Timer stopped externally
                }
                whiteTimeRemaining[0]--;
            }
            if (whiteTimeRemaining[0] == 0) {
                Platform.runLater(() -> declareWinner("Black"));  // White timed out => Black wins
            }
        });
        whiteTimerThread.setDaemon(true);
        whiteTimerThread.start();
    }

    private void startBlackTimer() {
        stopBlackTimer();  // Stop previous if running
        final int[] blackTimeRemaining = {1 * 60}; // example: 5 minutes

        blackTimerThread = new Thread(() -> {
            while (blackTimeRemaining[0] > 0 && !isWhiteTurn) {
                updateTimerLabel(blackPlayerLabel, blackTimeRemaining[0], "Black");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return; // Timer stopped externally
                }
                blackTimeRemaining[0]--;
            }
            if (blackTimeRemaining[0] == 0) {
                Platform.runLater(() -> declareWinner("White"));  // Black timed out => White wins
            }
        });
        blackTimerThread.setDaemon(true);
        blackTimerThread.start();
    }

    private void stopWhiteTimer() {
        whiteTimerRunning=false;
        if (whiteTimerThread != null && whiteTimerThread.isAlive()) {
            whiteTimerThread.interrupt();
        }
    }

    private void stopBlackTimer() {
        blackTimerRunning=false;
        if (blackTimerThread != null && blackTimerThread.isAlive()) {
            blackTimerThread.interrupt();
        }
    }

    private String whitePlayer;
    private String blackPlayer;

    private int loggedInUserId;      // For returning to dashboard
    private String loggedInUsername;
    public void setLoggedInUsername(String username) {
        this.loggedInUsername = username;
        if (boardOwnerLabel != null) {
            String role = username.equals(whitePlayer) ? "White" : "Black";
            String color = role.equals("White") ? "green" : "blue";
            boardOwnerLabel.setText("This board belongs to: " + username + " (" + role + ")");
            boardOwnerLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        }

    }

    public void setPlayers(String whitePlayer, String blackPlayer, int whitePlayerId, int blackPlayerId) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        if (loggedInUsername != null) {
            boardOwnerLabel.setText("B:" + loggedInUsername);
        }

        if (loggedInUsername.equals(whitePlayer)) {
            localPlayerColor = ChessPiece.Color.WHITE;
            isFlipped = false;
        } else {
            localPlayerColor = ChessPiece.Color.BLACK;
            isFlipped = true;
        }

        whitePlayerLabel.setText("White: " + whitePlayer);
        blackPlayerLabel.setText("Black: " + blackPlayer);
        updateTurnLabel();

        // 🧠 Setup the board only after knowing the player color
        setupPieces();
        drawBoard();

    }

    private void updateTurnLabel() {
        if (isWhiteTurn) {
            playerTurnLabel.setText("Turn: White (" + whitePlayer + ")");
        } else {
            playerTurnLabel.setText("Turn: Black (" + blackPlayer + ")");
        }
    }

    public void setPlayerNames(String whitePlayer, String blackPlayer) {
        whitePlayerLabel.setText("White: " + whitePlayer);
        blackPlayerLabel.setText("Black: " + blackPlayer);
    }


    private void declareWinner(String winnerName) {

        if (gameEnded) return;  // ✅ prevent double result
        gameEnded = true;
        chessBoard.setDisable(true);

        ChessPiece.Color winnerColor = winnerName.equals("White") ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;

        if ((winnerColor == ChessPiece.Color.WHITE && localPlayerColor == ChessPiece.Color.WHITE) ||
                (winnerColor == ChessPiece.Color.BLACK && localPlayerColor == ChessPiece.Color.BLACK)) {
            updatePlayerStats(winnerColor);  // ✅ update only ONCE
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Time Up");
        alert.setHeaderText("Time is up!");
        alert.setContentText(winnerName + " wins due to timeout!");
        alert.getButtonTypes().setAll(ButtonType.OK);

        alert.showAndWait().ifPresent(response -> {
            closeGameWindow(); // ✅
        });

    }




    @FXML
    private void onHomeButtonClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/mychess/home.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) chessBoard.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Home");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onDashboardButtonClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/mychess/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.initializeUser(loggedInUserId, loggedInUsername);
            Stage stage = (Stage) chessBoard.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Player Dashboard");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private com.example.mychess.SocketClient socketClient;

    public void setSocketClient(SocketClient socketClient) {
        this.socketClient = socketClient;

        socketClient.addListener(new SocketClient.MessageListener() {
            @Override
            public void onChallengeReceived(String fromUser) {
                // Not needed here in game screen
            }

            @Override
            public void onChallengeResponse(String fromUser, String response) {
                // Not needed here in game screen
            }

            @Override
            public void onMoveReceived(String fromUser, String moveData) {
                System.out.println("Received move from " + fromUser + ": " + moveData);
                if (fromUser.equals(opponentUsername)) {

                    Platform.runLater(() -> processOpponentMove(moveData));
                }
            }

            @Override
            public void onStartGame(String opponentUsername) {

            }
            @Override
            public void onPlayerNotAvailable(String opponentUsername){

            }
            @Override
            public void onChallengeAcknowledged(String opponentUsername) {

            }

        });
    }

    void processOpponentMove(String moveData) {
        // Example moveData format: "fromRow,fromCol,toRow,toCol"
        String[] parts = moveData.split(",");
        if (parts.length != 4) {
            System.err.println("Invalid move data: " + moveData);
            return;
        }
        try {
            int fromRow = Integer.parseInt(parts[0]);
            int fromCol = Integer.parseInt(parts[1]);
            int toRow = Integer.parseInt(parts[2]);
            int toCol = Integer.parseInt(parts[3]);

            // Now apply this move to your game board
            // For example, call your existing move logic method:
            System.out.println("[GameController] Opponent move data: " + moveData);
            movePiece(fromRow, fromCol, toRow, toCol,false);

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void makeMove(int fromRow, int fromCol, int toRow, int toCol) {
        // Your existing logic to update board state locally
        // e.g., update piece positions, redraw UI, etc.
    }



    private void sendMoveToOpponent(int fromRow, int fromCol, int toRow, int toCol) {
        if (socketClient != null && opponentUsername != null) {

            String moveStr = fromRow + "," + fromCol + "," + toRow + "," + toCol;
            socketClient.sendMessage("MOVE:" + loggedInUsername + ":" + opponentUsername + ":" + moveStr);
            System.out.println("[GameController] sendMoveToOpponent: MOVE:" + loggedInUsername + ":" + opponentUsername + ":" + moveStr);
        }
    }


    private void handleSocketMessage(String message) {
        Platform.runLater(() -> {
            if (message.startsWith("MOVE:")) {
                // Example MOVE message: MOVE:fromRow,fromCol,toRow,toCol
                String moveData = message.substring("MOVE:".length());
                String[] parts = moveData.split(",");
                int fromRow = Integer.parseInt(parts[0]);
                int fromCol = Integer.parseInt(parts[1]);
                int toRow = Integer.parseInt(parts[2]);
                int toCol = Integer.parseInt(parts[3]);

                // Apply opponent's move on the board, do not resend this move
                movePiece(fromRow, fromCol, toRow, toCol, false);
            }
        });
    }

   // private String opponentUsername;

    public void setOpponentUsername(String username) {
        this.opponentUsername = username;
    }

    private DashboardController dashboardController;

    public void setDashboardController(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }



    // public void onStopgame(ActionEvent actionEvent) {

    //}
}