import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_GAMES = 5;
    private static final int PLAYERS_PER_GAME = 2;
    private static final int MAX_LEVEL_DIFFERENCE = 6; // Changed from 10 to 7
    private static List<User> ingamePlayers = new ArrayList<>();
    private static List<OngoingGames> ongoingGames = new ArrayList<>();

    public static final ExecutorService gameExecutor = Executors.newFixedThreadPool(MAX_GAMES);

    private static final Lock lock = new ReentrantLock();
    private static final Condition gameReadyCondition = lock.newCondition();
    private static final CompletionService<Socket> completionService = new ExecutorCompletionService<>(gameExecutor);

    public static void main(String[] args) {
        ExecutorService gameExecutor = Executors.newFixedThreadPool(MAX_GAMES);
        List<User> waitingUsers = new ArrayList<>();
        int waitingUsersSize = 0;

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
                for (OngoingGames currGame : ongoingGames) {
                    if (currGame.getGameResult().isDone() || currGame.getGame().isGameFinished()) {
                        try {
                            Socket winnerSocket = currGame.getGameResult().get();
                            if (currGame.getRanked()) {
                                if (winnerSocket == null) {
                                    for (User player : currGame.getPlayers()) {
                                        player.setLevel(player.getLevel() + 3);
                                    }
                                } else {
                                    for (User player : currGame.getPlayers()) {
                                        if (player.getSocket().equals(winnerSocket)) {
                                            player.setLevel(player.getLevel() + 5);
                                        } else {
                                            player.setLevel(player.getLevel() + 1);
                                        }
                                    }
                                }
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        } finally {
                            ongoingGames.remove(currGame);
                            System.out.println("\n\n------------ Game ended ---------------\n\n");
                            for (User player : currGame.getPlayers()) {
                                waitingUsers.add(player);
                            }
                            ingamePlayers.removeAll(currGame.getPlayers());
                        }
                    }
                }

                if (waitingUsers.size() != waitingUsersSize) {
                    System.out.println("--------------------");
                    System.out.println(waitingUsers);
                    System.out.println("--------------------");
                    waitingUsersSize = waitingUsers.size();
                }

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
                                    // System.out.println("waitingUsers before: " + waitingUsers);
                                    waitingUsers.remove(firstUser);
                                    waitingUsers.remove(secondUser);
                                    // System.out.println("waitingUsers after: " + waitingUsers);
                                    waitingUsersSize = waitingUsers.size();
                                    System.out.println("\n\ningameplayers before: " + ingamePlayers);
                                    ingamePlayers.add(firstUser);
                                    ingamePlayers.add(secondUser);
                                    System.out.println("ingameplayers after: " + ingamePlayers + "\n\n");
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
                                        waitingUsers.remove(firstUser);
                                        waitingUsers.remove(secondUser);
                                        waitingUsersSize = waitingUsers.size();
                                        ingamePlayers.add(firstUser);
                                        ingamePlayers.add(secondUser);
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

                Socket socket = serverSocket.accept();
                User user = new User(socket);

                boolean ingameUserFound = false;

                System.out.println("user: " + user.getUsername());
                System.out.println("ingamePlayers: ");
                for (User usr : ingamePlayers) {
                    System.out.println(usr.getUsername());
                }
                System.out.println("\n\n");

                for (User ingameUser : ingamePlayers) { // fault tolerance
                    if (ingameUser.getUsername().equals(user.getUsername())) {
                        System.out.println("\n\nalready connected:" + socket.getLocalPort() + "\n\n");
                        ingameUserFound = true;
                        user.close();
                        break;
                    }
                }

                if (user.authenticate() && !ingameUserFound) {
                    System.out.println("\n\nwaitingUsers: " + waitingUsers + "\n\n");
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
            // lock.lock();

            try {
                Game game = new Game(PLAYERS_PER_GAME, userSockets, usernames);

                Future<Socket> gameResult = gameExecutor.submit(game::start);
                // gameReadyCondition.signal();
                OngoingGames currentGame = new OngoingGames(game, players, gameResult, ranked);
                ongoingGames.add(currentGame);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            } finally {
                // lock.unlock();
            }

        } catch (Exception e) {
            System.err.println("Error getting game result: " + e.getMessage());
        }
    }
}
