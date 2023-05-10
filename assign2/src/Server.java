import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_GAMES = 5;
    private static final int PLAYERS_PER_GAME = 2;
    private static final int MAX_WAIT_TIME = 60000;
    private static final int MAX_LEVEL_DIFFERENCE = 6;  // Changed from 10 to 7

    public static void main(String[] args) {
    ExecutorService gameExecutor = Executors.newFixedThreadPool(MAX_GAMES);
    List<User> waitingUsers = new ArrayList<>();

    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
        System.out.println("Server is running...");
        while (true) {
            Socket socket = serverSocket.accept();
            User user = new User(socket);

            if (user.authenticate()) {
                waitingUsers.add(user);
                if (waitingUsers.size() >= PLAYERS_PER_GAME) {
                    for (int i = 0; i < waitingUsers.size(); i++) {
                        User firstUser = waitingUsers.get(i);
                        List<User> players = new ArrayList<>();
                        players.add(firstUser);

                        for (int j = i + 1; j < waitingUsers.size(); j++) {
                            User secondUser = waitingUsers.get(j);
                            if (Math.abs(firstUser.getLevel() - secondUser.getLevel()) <= MAX_LEVEL_DIFFERENCE) {
                                players.add(secondUser);
                                if (players.size() == PLAYERS_PER_GAME) {
                                    List<Socket> userSockets = new ArrayList<>();
                                    for (User player : players) {
                                        userSockets.add(player.getSocket());
                                    }
                                    Game game = new Game(PLAYERS_PER_GAME, userSockets);
                                    try {
                                        Socket winnerSocket = gameExecutor.submit(game::start).get();

                                        if (winnerSocket == null) {
                                            for (User player : players) {
                                                player.setLevel(player.getLevel() + 3);
                                            }
                                        } else {
                                            for (User player : players) {
                                                if (player.getSocket().equals(winnerSocket)) {
                                                    player.setLevel(player.getLevel() + 5);
                                                } else {
                                                    player.setLevel(player.getLevel() + 1);
                                                }
                                            }
                                        }
                                    } catch (InterruptedException | ExecutionException e) {
                                        System.err.println("Error getting game result: " + e.getMessage());
                                    }

                                    waitingUsers.removeAll(players);
                                    break;
                                }
                            }
                        }

                        if (players.size() == PLAYERS_PER_GAME) {
                            break;
                        }
                    }
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
