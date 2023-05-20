import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_GAMES = 5;
    private static final int PLAYERS_PER_GAME = 2;
    private static final int MAX_LEVEL_DIFFERENCE = 6; // Changed from 10 to 7

    public static void main(String[] args) {
        ExecutorService gameExecutor = Executors.newFixedThreadPool(MAX_GAMES);
        List<User> waitingUsers = new ArrayList<>();

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

                if (user.authenticate()) {
                    boolean userFound = false;
                    for (User waitingUser : waitingUsers) { // fault tolerance
                        if (waitingUser.getUsername().equals(user.getUsername())) {
                            userFound = true;
                            waitingUser.close();
                            waitingUser.setSocket(socket);
                            break;
                        }
                    }
                    if (!userFound) {
                        waitingUsers.add(user);
                    }

                    System.out.println("User " + user.getUsername() + " connected from " + socket.getInetAddress()
                            .getHostAddress() + ":" + socket.getPort());

                    if (waitingUsers.size() >= PLAYERS_PER_GAME) {
                        for (int i = 0; i < waitingUsers.size(); i++) {
                            User firstUser = waitingUsers.get(i);
                            List<User> players = new ArrayList<>();
                            players.add(firstUser);

                            if (matchmaking == 1) {
                                for (int j = i + 1; j < waitingUsers.size(); j++) {
                                    User secondUser = waitingUsers.get(j);
                                    players.add(secondUser);
                                    if (players.size() == PLAYERS_PER_GAME) {
                                        startGame(players, gameExecutor, waitingUsers, false);
                                        break;
                                    }
                                }
                            } else if (matchmaking == 2) {
                                for (int j = i + 1; j < waitingUsers.size(); j++) {
                                    User secondUser = waitingUsers.get(j);
                                    if (Math.abs(
                                            firstUser.getLevel() - secondUser.getLevel()) <= MAX_LEVEL_DIFFERENCE) {
                                        players.add(secondUser);
                                        if (players.size() == PLAYERS_PER_GAME) {
                                            startGame(players, gameExecutor, waitingUsers, true);
                                            break;
                                        }
                                    }
                                }

                                if (players.size() == PLAYERS_PER_GAME) {
                                    break;
                                }
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

    private static void startGame(List<User> players, ExecutorService gameExecutor, List<User> waitingUsers,
            Boolean ranked) {
        List<Socket> userSockets = new ArrayList<>();
        for (User player : players) {
            userSockets.add(player.getSocket());
            System.out.println("User " + player.getUsername() + " socket " + player.getSocket());
        }
        List<String> usernames = new ArrayList<>();
        for (User player : players) {
            usernames.add(player.getUsername());
        }
        try {
            Game game = new Game(PLAYERS_PER_GAME, userSockets, usernames);

            // Socket winnerSocket = gameExecutor.submit(game::start).get();

            Thread gameThread = new Thread(game::start);
            gameThread.start();

            Socket winnerSocket = game.getWinnerSocket();

            if (ranked) {
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
            }
        } catch (Exception e) {
            System.err.println("Error getting game result: " + e.getMessage());
        }

        waitingUsers.removeAll(players);
    }
}
