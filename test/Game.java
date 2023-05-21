import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class Game {
  private static List<User> players;
  private static List<OutputStream> userOutputs;
  private char[][] board;
  private static final int ROWS = 6;
  private static final int COLUMNS = 7;
  private static final char EMPTY = '.';
  private static final char[] PLAYERS = { 'X', 'O' };
  private User winner = null;
  private User loser;
  private boolean isFinished = false;

  public Game(List<User> players) {
    this.players = players;
    try {
      for (int i = 0; i < players.size(); i++) {
        this.userOutputs.add(players.get(i).getSocket().getOutputStream());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    this.board = new char[ROWS][COLUMNS];
    initializeBoard(this.board);
  }

  public User start() {
    sendToAll("Your game is starting now!");
    printBoard(this.board);

    int currentPlayerIndex = 0;
    int moveCount = 0;
    boolean gameWon = false;

    while (!gameWon && moveCount < ROWS * COLUMNS) {
      char currentPlayer = PLAYERS[currentPlayerIndex];
      User currentUser = players.get(currentPlayerIndex);
      int column = -1;

      try {
        User otherUser = players.get((currentPlayerIndex + 1) % PLAYERS.length);
        sendMessageToPlayer(otherUser, "Opponent is playing...");
        column = getColumnFromPlayer(currentUser, currentPlayer);
      } catch (IOException e) {
        this.winner = players.get((currentPlayerIndex + 1) % PLAYERS.length);
        this.loser = currentUser;
        this.isFinished = true;
        // sendGameResult(this.winner, "OPPONENT_DISCONNECTED");
        closeSockets();
        return this.winner;
      }

      if (column >= 0 && column < COLUMNS && board[0][column] == EMPTY) {
        int row = makeMove(this.board, column, currentPlayer);
        printBoard(this.board);

        gameWon = checkWin(this.board, row, column);
        if (gameWon) {
          this.winner = currentUser;
          this.loser = players.get((currentPlayerIndex + 1) % PLAYERS.length);
          this.isFinished = true;
          sendMessageToPlayer(this.winner, "You won!");
          sendMessageToPlayer(this.loser, "You lost!");
          closeSockets();
          return this.winner;
        }

        moveCount++;
        currentPlayerIndex = (currentPlayerIndex + 1) % PLAYERS.length;
      } else {
        sendMessageToPlayer(currentUser, "Invalid move. Try again.");
      }
    }

    this.isFinished = true;

    if (!gameWon) {
      sendToAll("It's a draw!");
      closeSockets();
      // sendGameResult("Playing again, returned to waiting queue", "Playing again,
      // returned to waiting queue");
      return null;
    } else {
      sendMessageToPlayer(this.winner, "You won!");
      sendMessageToPlayer(this.loser, "You lost!");
      closeSockets();
      // sendGameResult("PLAY_AGAIN", "PLAY_AGAIN");
      return winner;
    }
  }

  private static void initializeBoard(char[][] board) {
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        board[row][col] = EMPTY;
      }
    }
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

  private void closeSockets() {
    for (User player : players) {
      try {
        player.getSocket().close();
      } catch (IOException e) {
        System.err.println("Error closing socket: " + e.getMessage());
      }
    }
  }

  private static void sendToAll(String message) {
    for (OutputStream output : userOutputs) {
      try {
        output.write(message.getBytes());
        output.flush();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static void sendMessageToPlayer(User player, String message) {
    try {
      player.getSocket().getOutputStream().write(message.getBytes());
      player.getSocket().getOutputStream().flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void printBoard(char[][] board) {
    for (int row = 0; row < ROWS; row++) {
      for (int col = 0; col < COLUMNS; col++) {
        sendToAll(board[row][col] + " ");
      }
      sendToAll("\n");
    }
    sendToAll("\n");
  }

  private int getColumnFromPlayer(User player, char symbol) throws IOException {
    BufferedReader in = new BufferedReader(new InputStreamReader(player.getSocket().getInputStream()));
    sendMessageToPlayer(player, "Your turn (Player X). Enter a column (0-6): ");
    return Integer.parseInt(in.readLine());
  }
}
