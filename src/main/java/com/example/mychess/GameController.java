package com.example.mychess;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.control.Alert;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;


public class GameController {
    private static final int INITIAL_TIME = 60;//(1min)
    @FXML
    private Label whitePlayerLabel;

    @FXML
    private Label blackPlayerLabel;

    @FXML
    private Label playerTurnLabel;

    private int whiteTime = 60; // 1 minute
    private int blackTime = 60;

    private Thread whiteTimerThread;
    private Thread blackTimerThread;

    private volatile boolean whiteTimerRunning = false;
    private volatile boolean blackTimerRunning = false;

    private boolean isWhiteTurn = true;

    //bbb
    //@FXML private Label playerTurnLabel;
    @FXML private GridPane chessBoard;

    private final int SIZE = 8;
    private StackPane[][] boardSquares = new StackPane[SIZE][SIZE];
    private ChessPiece[][] boardPieces = new ChessPiece[SIZE][SIZE];
    //private ChessPiece[][] board = new ChessPiece[8][8];


    //private boolean isWhiteTurn = true;
    private int selectedRow = -1, selectedCol = -1;

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

    private void updateTimerLabel(Label label, int timeLeft, String playerName) {
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);
        Platform.runLater(() -> label.setText(playerName + ": " + timeStr));
    }

    private void setupPieces() {
        // Black pieces
        boardPieces[0] = new ChessPiece[]{
                new ChessPiece(ChessPiece.Type.ROOK, ChessPiece.Color.BLACK),
                new ChessPiece(ChessPiece.Type.KNIGHT, ChessPiece.Color.BLACK),
                new ChessPiece(ChessPiece.Type.BISHOP, ChessPiece.Color.BLACK),
                new ChessPiece(ChessPiece.Type.QUEEN, ChessPiece.Color.BLACK),
                new ChessPiece(ChessPiece.Type.KING, ChessPiece.Color.BLACK),
                new ChessPiece(ChessPiece.Type.BISHOP, ChessPiece.Color.BLACK),
                new ChessPiece(ChessPiece.Type.KNIGHT, ChessPiece.Color.BLACK),
                new ChessPiece(ChessPiece.Type.ROOK, ChessPiece.Color.BLACK)
        };
        for (int i = 0; i < SIZE; i++) {

            boardPieces[1][i] = new ChessPiece(ChessPiece.Type.PAWN, ChessPiece.Color.BLACK);
        }

        // White pieces
        boardPieces[7] = new ChessPiece[]{
                new ChessPiece(ChessPiece.Type.ROOK, ChessPiece.Color.WHITE),
                new ChessPiece(ChessPiece.Type.KNIGHT, ChessPiece.Color.WHITE),
                new ChessPiece(ChessPiece.Type.BISHOP, ChessPiece.Color.WHITE),
                new ChessPiece(ChessPiece.Type.QUEEN, ChessPiece.Color.WHITE),
                new ChessPiece(ChessPiece.Type.KING, ChessPiece.Color.WHITE),
                new ChessPiece(ChessPiece.Type.BISHOP, ChessPiece.Color.WHITE),
                new ChessPiece(ChessPiece.Type.KNIGHT, ChessPiece.Color.WHITE),
                new ChessPiece(ChessPiece.Type.ROOK, ChessPiece.Color.WHITE)
        };
        for (int i = 0; i < SIZE; i++)
        {boardPieces[6][i] = new ChessPiece(ChessPiece.Type.PAWN, ChessPiece.Color.WHITE);
            }
    }

    private void drawBoard() {
        chessBoard.getChildren().clear();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                StackPane square = createSquare(row, col);
                boardSquares[row][col] = square;
                chessBoard.add(square, col, row);
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
        square.setOnMouseClicked(e -> handleClick(row, col));

        return square;
    }
    /*private void showWinDialog(ChessPiece.Color winner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText(winner + " wins!");
        alert.showAndWait();
    }*/
    private void showWinDialog(ChessPiece.Color winner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText("King is Blocked – No Movement");
        alert.setContentText( winner == ChessPiece.Color.WHITE ? "White Colour wins the game. Congratulations!" : "Black wins the game. Congratulations!"
        );

         alert.showAndWait();
        resetGame();
    }

    private void resetGame() {
        // Clear the board logic
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                boardPieces[row][col] = null;

                // Clear UI squares
                StackPane square = boardSquares[row][col];
                if (square != null) {
                    square.getChildren().removeIf(node -> node instanceof Label); // remove piece labels
                }
            }
        }

        // Reset game logic
        isWhiteTurn = true;

        // Setup fresh board
        setupPieces();         // put pieces in initial position
        updatePlayerTurn();    // update turn label
        int whiteTimeRemaining = 1 * 60;
       int blackTimeRemaining = 1 * 60;

        stopWhiteTimer();
        stopBlackTimer();

        startWhiteTimer();
    }




    private void movePiece(int fromRow, int fromCol, int toRow, int toCol) {
        ChessPiece movingPiece = boardPieces[fromRow][fromCol];
        ChessPiece targetPiece = boardPieces[toRow][toCol];

        // Prevent capturing own piece or king
        if (targetPiece != null && targetPiece.getColor() == movingPiece.getColor()) {
            return;
        }
        if (targetPiece != null && targetPiece.getType() == ChessPiece.Type.KING) {
            return; // King ke khowa jabena
        }

        // Move the piece
        boardPieces[toRow][toCol] = movingPiece;
        boardPieces[fromRow][fromCol] = null;

        // akta movement er pore board/ui ke update korbe and new player er turn asbe
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


        // king is captured tahole show win dekhabe
        if (isKingCaptured()) {
            ChessPiece.Color winner = movingPiece.getColor() == ChessPiece.Color.WHITE ? ChessPiece.Color.BLACK : ChessPiece.Color.WHITE;
            showWinDialog(winner);
        }

      
        // Switch turns (you probably already do this)
        //isWhiteTurn = !isWhiteTurn;

// turn change howar pore kaj, check for checkmate
        ChessPiece.Color currentPlayer = isWhiteTurn ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK;

        if (isKingInCheck(currentPlayer) && !hasAnyValidMove(currentPlayer)) {
            showWinDialog(isWhiteTurn ? ChessPiece.Color.BLACK : ChessPiece.Color.WHITE); // Previous player wins
        }

    }

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



    private void handleClick(int row, int col) {
        ChessPiece clicked = boardPieces[row][col];

        // Select a piece if no piece is selected and it's the correct player's turn
        if (selectedRow == -1 && clicked != null && clicked.getColor() == (isWhiteTurn ? ChessPiece.Color.WHITE : ChessPiece.Color.BLACK)) {
            selectedRow = row;
            selectedCol = col;
            boardSquares[row][col].setStyle("-fx-border-color: red; -fx-border-width: 3;");
        }
        // Try to move the selected piece
        else if (selectedRow != -1) {
            if (isValidMove(selectedRow, selectedCol, row, col)) {
                movePiece(selectedRow, selectedCol, row, col);
            }
            // Clear selection
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

        // Bishop movement logic (diagonal movement)
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
                    return true; // Capture
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

    private void updatePlayerTurn() {
        if (playerTurnLabel != null) {
            if (isWhiteTurn) {
                playerTurnLabel.setText("White's Turn");
                stopBlackTimer();
                blackTime = INITIAL_TIME; // reset black timer

                whiteTime = INITIAL_TIME; // reset white timer
                startWhiteTimer();
            } else {
                playerTurnLabel.setText("Black's Turn");
                stopWhiteTimer();
                whiteTime = INITIAL_TIME; // reset white timer

                blackTime = INITIAL_TIME; // reset black timer
                startBlackTimer();
            }
        }
        updateTimerLabel(whitePlayerLabel, whiteTime, "White");
        updateTimerLabel(blackPlayerLabel, blackTime, "Black");

    }

    private void startWhiteTimer() {
        stopWhiteTimer(); // Stop previous thread jodi thake
        final int[] whiteTimeRemaining = {1 * 60}; // Reset to 5 mins
        Thread whiteTimer = new Thread(() -> {
            while (whiteTimeRemaining[0] > 0 && isWhiteTurn) {
                int minutes = whiteTimeRemaining[0] / 60;
                int seconds = whiteTimeRemaining[0] % 60;
                Platform.runLater(() -> whitePlayerLabel.setText("White: " + String.format("%02d:%02d", minutes, seconds)));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return; // Timer stopped externally
                }
                whiteTimeRemaining[0]--;
            }
            if (whiteTimeRemaining[0] == 0) {
                Platform.runLater(() -> declareWinner("Black"));
            }
        });
        whiteTimer.setDaemon(true);
        whiteTimer.start();
    }

    private void declareWinner(String winner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(null);
        alert.setContentText(winner + " wins! Time is up.");

        ButtonType okButton = new ButtonType("Play Again", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);

        alert.showAndWait().ifPresent(response -> {
           // if (response == okButton) {
                resetGame();
            //}
        });
    }




    private void startBlackTimer() {
        stopBlackTimer(); // Stop previous thread if running
        final int[] blackTimeRemaining = {1 * 60}; // Reset to 5 minutes
        Thread blackTimer = new Thread(() -> {
            while (blackTimeRemaining[0] > 0 && !isWhiteTurn) {
                int minutes = blackTimeRemaining[0] / 60;
                int seconds = blackTimeRemaining[0] % 60;
                Platform.runLater(() -> blackPlayerLabel.setText("Black: " + String.format("%02d:%02d", minutes, seconds)));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    return;
                }
                blackTimeRemaining[0]--;
            }
            if (blackTimeRemaining[0] == 0) {
                Platform.runLater(() -> declareWinner("White"));
            }
        });
        blackTimer.setDaemon(true);
        blackTimer.start();
    }


    private void stopWhiteTimer() {
        whiteTimerRunning = false;
        if (whiteTimerThread != null) {
            whiteTimerThread.interrupt();
        }
    }
    private void stopBlackTimer() {
        blackTimerRunning = false;
        if (blackTimerThread != null) {
            blackTimerThread.interrupt();
        }
    }

    private String whitePlayer;
    private String blackPlayer;
    public void setPlayers(String whitePlayer, String blackPlayer) {
        this.whitePlayer = whitePlayer;
        this.blackPlayer = blackPlayer;
        whitePlayerLabel.setText("White: " + whitePlayer);
        blackPlayerLabel.setText("Black: " + blackPlayer);
        playerTurnLabel.setText("Turn: " + whitePlayer);
    }

    public void setPlayerNames(String whitePlayer, String blackPlayer) {
        whitePlayerLabel.setText("White: " + whitePlayer);
        blackPlayerLabel.setText("Black: " + blackPlayer);
    }

}
