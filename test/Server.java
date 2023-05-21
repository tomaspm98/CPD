import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class Server {
  private static final int PORT = 12345;
  private static final int MAX_GAMES = 5;
  private static final int PLAYERS_PER_GAME = 2;
  private static final int MAX_LEVEL_DIFFERENCE = 6;
  private static ExecutorService gameExecutor = Executors.newFixedThreadPool(MAX_GAMES);
  private static Map<User, Long> waitingUsers = new HashMap<>();

  public static void main(String[] args) {
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      Scanner scanner = new Scanner(System.in);
      int matchmaking;

      while (true) {
        System.out.print(
            "There are two options for the matchmaking: \n1. Simple - Rank is irrelevant \n2. Ranked - Can only match with players of similar rank \nPlease enter your choice: ");
        matchmaking = scanner.nextInt();
        if (matchmaking == 1) {
          System.out.println("Simple matchmaking selected.");
          break;
        } else if (matchmaking == 2) {
          System.out.println("Ranked matchmaking selected.");
          break;
        } else {
          System.out.println("Invalid choice. Simple matchmaking selected.");
        }
      }
      scanner.close();
      System.out.println("Server is running...");

      while (true) {
        Socket socket = serverSocket.accept();
        User user = new User(socket);
        boolean userFound = false;

        if (user.authenticate()) {
          System.out.println("New user connected: " + user.getUsername());
          for (User waitingUser : waitingUsers.keySet()) {
            if (waitingUser.getUsername().equals(user.getUsername())) {
              userFound = true;
              waitingUser.close();
              waitingUser.setSocket(socket);
              break;
            }
            if (!userFound) {
              waitingUsers.put(user, System.currentTimeMillis());
            }
          }
        } else {
          user.close();
        }

        if (waitingUsers.size() >= PLAYERS_PER_GAME) {
          for (User firstUser : waitingUsers.keySet()) {
            List<User> players = new ArrayList<>();
            players.add(firstUser);
            for (User secondUser : waitingUsers.keySet()) {
              if (firstUser != secondUser) {
                if (matchmaking == 1) {
                  players.add(secondUser);
                  waitingUsers.remove(firstUser);
                  waitingUsers.remove(secondUser);
                  Game game = new Game(players);
                  gameExecutor.submit(game::start);
                  break;
                } else if (matchmaking == 2) {
                  // TODO: Implement ranked matchmaking
                }
              }
            }
          }
        }
      }
    } catch (IOException e) {
      System.err.println("Error starting server: " + e.getMessage());
    } finally {
      gameExecutor.shutdown();
    }
  }
}
