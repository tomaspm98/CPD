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
    private static final int MAX_LEVEL_DIFFERENCE = 6; // Changed from 10 to 7
    private static List<User> ingamePlayers = new ArrayList<>();
    private static List<OngoingGames> ongoingGames = new ArrayList<>();

    public static final ExecutorService gameExecutor = Executors.newFixedThreadPool(MAX_GAMES);

    private static final Lock lock = new ReentrantLock();
    private static final Condition gameReadyCondition = lock.newCondition();
    private static final CompletionService<Socket> completionService = new ExecutorCompletionService<>(gameExecutor);

    public static void main(String[] args) {
        Map<User, Long> waitingUsers = new HashMap<>();
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
                                waitingUsers.put(player, System.currentTimeMillis());
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
                    for (User firstUser : waitingUsers.keySet()) {
                        List<User> players = new ArrayList<>();
                        players.add(firstUser);

                        if (matchmaking == 1) {
                            for (User secondUser : waitingUsers.keySet()) {
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
                            for (User secondUser : waitingUsers.keySet()) {
                                if (firstUser != secondUser) {
                                    long waitTime = System.currentTimeMillis()
                                            - Math.max(waitingUsers.get(firstUser), waitingUsers.get(secondUser));
                                    int currentMaxLevelDifference = MAX_LEVEL_DIFFERENCE + (int) (waitTime / 10000) * 2;
                                    if (Math.abs(
                                            firstUser.getLevel()
                                                    - secondUser.getLevel()) <= currentMaxLevelDifference) {
                                        players.add(secondUser);
                                        if (players.size() == PLAYERS_PER_GAME) {
                                            for (User player : players) {
                                                player.sendMessage("You are now entering a game. Good luck!");
                                            }

                                            try {
                                                Thread.sleep(2000); // Wait for 2 seconds before starting the game
                                            } catch (InterruptedException e) {
                                                System.err.println("Error in sleep: " + e.getMessage());
                                            }

                                            for (User player : players) {
                                                for (User opponent : players) {
                                                    if (!player.equals(opponent)) {
                                                        player.sendOpponentDetails(opponent);
                                                    }
                                                }
                                            }

                                            try {
                                                Thread.sleep(2000); // Wait for 2 seconds before starting the game
                                            } catch (InterruptedException e) {
                                                System.err.println("Error in sleep: " + e.getMessage());
                                            }

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
                            }
                        }
                        if (players.size() == PLAYERS_PER_GAME) {
                            break;
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
                    System.out.println("\n\nwaitingUsers: " + waitingUsers.keySet() + "\n\n");
                    boolean userFound = false;
                    for (User waitingUser : waitingUsers.keySet()) { // fault tolerance
                        if (waitingUser.getUsername().equals(user.getUsername())) {
                            userFound = true;
                            waitingUser.close();
                            waitingUser.setSocket(socket);
                            break;
                        }
                    }
                    if (!userFound) {
                        waitingUsers.put(user, System.currentTimeMillis());
                    }

                    System.out.println("User " + user.getUsername() + " connected from " + socket.getInetAddress()
                            .getHostAddress() + ":" + socket.getPort());
                } else {
                    user.close();
                }
                try {
                    Thread.sleep(1000); // Wait for 1 second before next check
                } catch (InterruptedException e) {
                    System.err.println("Error in sleep: " + e.getMessage());
                }
            }
        } catch (

        IOException e) {
            System.err.println("Error in server: " + e.getMessage());
        } finally {
            gameExecutor.shutdown();
        }
    }

    private static void startGame(List<User> players, ExecutorService gameExecutor, Map<User, Long> waitingUsers,
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

    private static void askForAnotherGame(List<User> players, Map<User, Long> waitingUsers) {
        for (User player : players) {
            try {
                PrintWriter out = new PrintWriter(player.getSocket().getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(player.getSocket().getInputStream()));

                out.println("Do you want to play another game? (yes/no)");
                out.flush();

                String response = in.readLine();
                if (response != null && response.equalsIgnoreCase("yes")) {
                    waitingUsers.put(player, System.currentTimeMillis());
                } else {
                    player.getSocket().close();
                }
            } catch (IOException e) {
                System.err.println("Error in askForAnotherGame: " + e.getMessage());
            }
        }
    }
}
