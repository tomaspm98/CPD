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
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

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
                    if (serverMessage == null) break;

                    if (serverMessage.startsWith("YOUR_TURN")) {
                        char player = serverMessage.charAt(serverMessage.length() - 1);
                        System.out.print("Your turn (Player " + player + "). Enter a column (0-6): ");
                        int column = scanner.nextInt();
                        out.println(column);
                    } else if ("WIN".equals(serverMessage)) {
                        System.out.println("You won!");
                        break;
                    } else if ("LOSE".equals(serverMessage)) {
                        System.out.println("You lost!");
                        break;
                    } else if ("DRAW".equals(serverMessage)) {
                        System.out.println("It's a draw!");
                        break;
                    } else if ("INVALID_MOVE".equals(serverMessage)) {
                        System.out.println("Invalid move. Try again.");
                    }
                }
            } else {
                System.out.println("Authentication failed.");
            }
        } catch (IOException e) {
            System.err.println("Error in client: " + e.getMessage());
        }
    }
}