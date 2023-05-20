import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class Game {
    private static List<Socket> userSockets;
    private static List<String> usernames;
    private char[][] board;
    private static final int ROWS = 6;
    private static final int COLUMNS = 7;
    private static final char EMPTY = '.';
    private static final char[] PLAYERS = { 'X', 'O' };
    private Socket winnerSocket;
    private Socket loserSocket;

    public Game(int players, List<Socket> userSockets, List<String> usernames) {
        this.userSockets = userSockets;
        this.usernames = usernames;
        this.board = new char[ROWS][COLUMNS];
        initializeBoard(this.board);
    }

    public Socket start() {
        System.out.println("Starting game with " + userSockets.size() + " players");
        printBoard(this.board);

        int currentPlayerIndex = 0;
        int moveCount = 0;
        boolean gameWon = false;

        while (!gameWon && moveCount < ROWS * COLUMNS) {
            char currentPlayer = PLAYERS[currentPlayerIndex];
            Socket currentSocket = userSockets.get(currentPlayerIndex);
            int column = -1;

            try {
                column = getColumnFromPlayer(currentSocket, currentPlayer);
            } catch (IOException e) {
                this.winnerSocket = userSockets.get((currentPlayerIndex + 1) % PLAYERS.length);
                sendGameResult(this.winnerSocket, "OPPONENT_DISCONNECTED");
                closeSockets();
                return this.winnerSocket;
            }

            if (column >= 0 && column < COLUMNS && board[0][column] == EMPTY) {
                int row = makeMove(this.board, column, currentPlayer);
                printBoard(this.board);

                gameWon = checkWin(this.board, row, column);
                if (gameWon) {
                    this.winnerSocket = currentSocket;
                    this.loserSocket = userSockets.get((currentPlayerIndex + 1) % PLAYERS.length);
                } else {
                    currentPlayerIndex = (currentPlayerIndex + 1) % PLAYERS.length;
                }
                moveCount++;
            } else {
                sendMessageToPlayer(currentSocket, "INVALID_MOVE");
            }
        }

        if (!gameWon) {
            sendGameResult("DRAW", "DRAW");
            closeSockets();
            return null;
        } else {
            sendGameResult(this.winnerSocket, "WIN");
            sendGameResult(this.loserSocket, "LOSE");
            closeSockets();
            return winnerSocket;
        }
    }

    private static void initializeBoard(char[][] board) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                board[row][col] = EMPTY;
            }
        }
    }

    private static void printBoard(char[][] board) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                System.out.print(board[row][col] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    private static int makeMove(char[][] board, int column, char player) {
        int row;
        for (row = ROWS - 1; row >= 0 && board[row][column] != EMPTY; row--)
            ;
        board[row][column] = player;
        return row;
    }

    private static boolean checkWin(char[][] board, int row, int col) {
        char player = board[row][col];

        // Check horizontal
        int count = 0;
        for (int c = 0; c < COLUMNS; c++) {
            count = (board[row][c] == player) ? count + 1 : 0;
            if (count >= 4)
                return true;
        }

        // Check vertical
        count = 0;
        for (int r = 0; r < ROWS; r++) {
            count = (board[r][col] == player) ? count + 1 : 0;
            if (count >= 4)
                return true;
        }

        // Check diagonal: top-left to bottom-right
        int startRow = row - Math.min(row, col);
        int startCol = col - Math.min(col, row);
        count = 0;
        for (int i = 0; i < Math.min(ROWS, COLUMNS); i++) {
            int r = startRow + i;
            int c = startCol + i;
            if (r < ROWS && c < COLUMNS) {
                count = (board[r][c] == player) ? count + 1 : 0;
                if (count >= 4)
                    return true;
            }
        }
        // Check diagonal: bottom-left to top-right
        startRow = row + Math.min(ROWS - row - 1, col);
        startCol = col - Math.min(ROWS - row - 1, col);
        count = 0;
        for (int i = 0; i < Math.min(ROWS, COLUMNS); i++) {
            int r = startRow - i;
            int c = startCol + i;
            if (r >= 0 && c < COLUMNS) {
                count = (board[r][c] == player) ? count + 1 : 0;
                if (count >= 4)
                    return true;
            }
        }

        return false;
    }

    private void sendMessageToPlayer(Socket socket, String message) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(message);
        } catch (IOException e) {
            System.err.println("Error sending message to player: " + e.getMessage());
        }
    }

    private int getColumnFromPlayer(Socket socket, char player) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        out.println("YOUR_TURN:" + player);
        return Integer.parseInt(in.readLine());
    }

    private void sendGameResult(String result1, String result2) {
        sendMessageToPlayer(userSockets.get(0), result1);
        sendMessageToPlayer(userSockets.get(1), result2);
    }

    private void sendGameResult(Socket socket, String result) {
        sendMessageToPlayer(socket, result);
    }

    private void closeSockets() {
        for (Socket socket : userSockets) {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    public static boolean containsUser(String user) {
        return usernames.contains(user);
    }

    public static void updateUserSocket(String user, Socket socket) {
        userSockets.set(usernames.indexOf(user), socket);
    }

    public Socket getWinnerSocket() {
        return this.winnerSocket;
    }
}