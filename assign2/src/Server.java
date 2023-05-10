import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 12345;
    private static final int MAX_GAMES = 5;
    private static final int PLAYERS_PER_GAME = 2; // You can change this value depending on the game requirements

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
                        gameExecutor.submit(game::start);
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