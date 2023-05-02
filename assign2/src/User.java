import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class User {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private static final UserDatabase userDatabase = new UserDatabase();

    public User(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public boolean authenticate() {
        try {
            String username = in.readLine();
            String password = in.readLine();

            // Add your authentication logic here. For example, you can  check the username and password against a database.
            // This example assumes that the username and password must be equal for successful authentication.
            if (userDatabase.authenticate(username, password)) {
                out.println("AUTH_SUCCESS");
                return true;
            } else {
                out.println("AUTH_FAILED");
                return false;
            }
        } catch (IOException e) {
            System.err.println("Error in user authentication: " + e.getMessage());
            return false;
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Error in closing user socket: " + e.getMessage());
        }
    }
}