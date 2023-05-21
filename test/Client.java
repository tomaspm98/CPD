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
      System.out.println("Host address: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());

      System.out.print("Enter your username: ");
      String username = scanner.nextLine();
      System.out.print("Enter your password: ");
      String password = scanner.nextLine();

      out.println(username);
      out.println(password);
      String response = in.readLine();

      if ("AUTH_SUCCESS".equals(response)) {
        System.out.println("Authentication successful. Waiting for a game...");
      }
    } catch (IOException e) {
      System.err.println("Error connecting to server: " + e.getMessage());
    }
  }
}
