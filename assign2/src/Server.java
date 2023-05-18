import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_GAMES = 5;
    private static final int PLAYERS_PER_GAME = 2;
    private static final int MAX_WAIT_TIME = 60000;
    private static final int MAX_LEVEL_DIFFERENCE = 6;  

    public static void main(String[] args) {
        ExecutorService gameExecutor = Executors.newFixedThreadPool(MAX_GAMES);
        Map<User,Long> waitingUsers = new HashMap<>();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running...");
            new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        User user = new User(socket);
                        int authStatus = user.authenticate();
                        if (authStatus == 1) {
                            waitingUsers.put(user, System.currentTimeMillis());
                    } else if (authStatus == -1) {
                        user.close();
                        }
                    } catch (IOException e) {
                        System.err.println("Error accepting new connection: " + e.getMessage());
                    }
                }
            }).start();
            while (true) {
                    if (waitingUsers.size() >= PLAYERS_PER_GAME) {
                        for (User firstUser : waitingUsers.keySet()) {
                            List<User> players = new ArrayList<>();
                            players.add(firstUser);

                            for (User secondUser : waitingUsers.keySet()) {
                                if (firstUser != secondUser) {
                                    long waitTime = System.currentTimeMillis() - Math.max(waitingUsers.get(firstUser), waitingUsers.get(secondUser));
                                    int currentMaxLevelDifference = MAX_LEVEL_DIFFERENCE + (int)(waitTime / 10000) * 2;
                                    //System.out.println("Wait time for users " + firstUser.getUsername() + " and " + secondUser.getUsername() + ": " + waitTime);
                                    //System.out.println("Current max level difference: " + currentMaxLevelDifference);

                                    if (Math.abs(firstUser.getLevel() - secondUser.getLevel()) <= currentMaxLevelDifference) {
                                        players.add(secondUser);
                                        if (players.size() == PLAYERS_PER_GAME) {
                                            for (User player : players) {
                                                player.sendMessage("You are now entering a game. Good luck!");
                                            }

                                            try {
                                                Thread.sleep(2000);  // Wait for 2 seconds before starting the game
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
                                                Thread.sleep(2000);  // Wait for 2 seconds before starting the game
                                            } catch (InterruptedException e) {
                                                System.err.println("Error in sleep: " + e.getMessage());
                                            }

                                            List<Socket> userSockets = new ArrayList<>();
                                            for (User player : players) {
                                                userSockets.add(player.getSocket());
                                            }
                                            Game game = new Game(PLAYERS_PER_GAME, userSockets);
                                            try {
                                                Socket winnerSocket = gameExecutor.submit(game::start).get();
                                                askForAnotherGame(players, waitingUsers);
                    
                                                if (winnerSocket == null) {
                                                    for (User player : players) {
                                                        player.setLevel(player.getLevel() + 3);
                                                        //checkForAnotherGame(player, waitingUsers);
                                                    }
                                                } else {
                                                    for (User player : players) {
                                                        if (player.getSocket().equals(winnerSocket)) {
                                                            player.setLevel(player.getLevel() + 5);
                                                        } else {
                                                            player.setLevel(player.getLevel() + 1);
                                                        }
                                                        //checkForAnotherGame(player, waitingUsers);
                                                    }
                                                }
                                            } catch (InterruptedException | ExecutionException e) {
                                                System.err.println("Error getting game result: " + e.getMessage());
}
                                        players.forEach(waitingUsers::remove);
                                        break;
                                    }
                                }
                            }
                        }

                        if (players.size() == PLAYERS_PER_GAME) {
                            break;
                        }
                    }
                }
            try {
                    Thread.sleep(1000);  // Wait for 1 second before next check
                } catch (InterruptedException e) {
                    System.err.println("Error in sleep: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error in server: " + e.getMessage());
        } finally {
            gameExecutor.shutdown();
    }
}

    private static void askForAnotherGame(List<User> players, Map<User,Long> waitingUsers) {
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

