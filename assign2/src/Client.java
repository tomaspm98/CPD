import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Scanner scanner = new Scanner(System.in)) {

            System.out.println("Host address: " + socket.getInetAddress().getHostAddress());
            System.out.println("Port number: " + socket.getPort());

            System.out.print("Enter your username: ");
            String username = scanner.nextLine();
            System.out.print("Enter your password: ");
            String password = scanner.nextLine();

            out.println(username);
            out.println(password);

            String response = in.readLine();

            if ("AUTH_SUCCESS".equals(response)) {
                System.out.println("Authentication successful. Waiting for a game...");
                while (true) {
                    String serverMessage = in.readLine();
                    if (serverMessage == null)
                        break;

                    if (serverMessage.startsWith("YOUR_TURN")) {
                        char player = serverMessage.charAt(serverMessage.length() - 1);
                        System.out.print("Your turn (Player " + player + "). Enter a column (0-6): ");
                        int column = scanner.nextInt();
                        out.println(column);
                    } else if ("WIN".equals(serverMessage)) {
                        System.out.println("You won!");
                    } else if ("LOSE".equals(serverMessage)) {
                        System.out.println("You lost!");
                    } else if ("DRAW".equals(serverMessage)) {
                        System.out.println("It's a draw!");
                        break;
                    } else if ("INVALID_MOVE".equals(serverMessage)) {
                        System.out.println("Invalid move. Try again.");
                    } else if ("OPPONENT_DISCONNECTED".equals(serverMessage)) {
                        System.out.println("Your opponent disconnected, you won!");
                        break;
                    } else if ("PLAY_AGAIN".equals(serverMessage)) {
                        System.out.print("Playing again, returned to waiting queue.\n");
                    }
                }
            } else if ("REGISTER?".equals(response)) {
                System.out.print(
                        "Authentication failed. Do you want to register a new account with these credentials? (yes/no): ");
                String registerResponse = scanner.nextLine();
                out.println(registerResponse);
            } else if ("REGISTER_SUCCESS".equals(response)) {
                System.out.println("Registration successful.");

            } else if ("REGISTER_FAIL".equals(response)) {
                System.out.println("Registration failed.");
            } else {
                System.out.println("Authentication failed.");
            }

        } catch (IOException e) {
            System.err.println("Error in client: " + e.getMessage());
        }
    }
}