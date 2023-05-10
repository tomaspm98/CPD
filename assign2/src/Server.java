import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_GAMES = 5;
    private static final int PLAYERS_PER_GAME = 2; // You can change this value depending on the game requirements
    private static final int MAX_WAIT_TIME = 60000;
    private static final int MAX_LEVEL_DIFFERENCE = 10;

    public static void main(String[] args) {
        ExecutorService gameExecutor = Executors.newFixedThreadPool(MAX_GAMES);
        List<User> waitingUsers = new ArrayList<>();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running...");
            while (true) {
                Socket socket = serverSocket.accept();
                User user = new User(socket);

                //String authResult = user.authenticate();
                if (user.authenticate()) {
                    waitingUsers.add(user);
                    if (waitingUsers.size() == PLAYERS_PER_GAME) {
                        List<Socket> userSockets = new ArrayList<>();
                        for (User waitingUser : waitingUsers) {
                        userSockets.add(waitingUser.getSocket());
                    }
                        Game game = new Game(PLAYERS_PER_GAME, userSockets);
                        try {
                        Socket winnerSocket = gameExecutor.submit(game::start).get();  // Capture the result of the game

                        if (winnerSocket == null) {  // Case of a draw
                            for (User waitingUser : waitingUsers) {
                                waitingUser.setLevel(waitingUser.getLevel() + 3);
                            }
                        } else {  // There's a winner
                            for (User waitingUser : waitingUsers) {
                                if (waitingUser.getSocket().equals(winnerSocket)) {
                                    waitingUser.setLevel(waitingUser.getLevel() + 5);  // Winner
                                } else {
                                    waitingUser.setLevel(waitingUser.getLevel() + 1);  // Loser
                                }
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        System.err.println("Error getting game result: " + e.getMessage());
                    }

                    waitingUsers.clear();
                }
            } else {
                user.close();
            }
            }
        } catch (IOException e) {
            System.err.println("Error in server: " + e.getMessage());
        } finally {
            gameExecutor.shutdown();
        }
    }
}