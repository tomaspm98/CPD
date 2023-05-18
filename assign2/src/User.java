import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class User {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private int level;
    private boolean playAgain;
    private static final UserDatabase userDatabase = new UserDatabase();

    public User(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.username = in.readLine();
        this.level = userDatabase.getLevel(username);
    }

    public int authenticate() {
        try {
            
            String password = in.readLine();
            
            
            if (!userDatabase.authenticate(username, password)) {
                out.println("REGISTER?");
                out.flush();
                String response = in.readLine();
                if ("yes".equalsIgnoreCase(response)) {
                    if (userDatabase.register(username, password)) {
                        out.println("REGISTER_SUCCESS");
                        this.level = userDatabase.getLevel(username);
                        return 1;
                    } else {
                        out.println("REGISTER_FAIL");
                        return -1;
                    }
                } else {
                    out.println("AUTH_FAILED");
                    return -1;
                }
            } else {
                out.println("AUTH_SUCCESS");
                return 1;
            }
        } catch (IOException e) {
            System.err.println("Error reading user credentials: " + e.getMessage());
            return -1;
        }
    }

    public String getUsername() {
        return this.username;
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
        userDatabase.setLevel(username, level);
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

    public void sendMessage(String message) {
    out.println(message);
    out.flush();  // Important to ensure that the message is actually sent
}

/*    public int getScore() {
    return userDatabase.getLevel(username);
}
*/
    public void sendOpponentDetails(User opponent) {
        sendMessage("Your opponent is " + opponent.getUsername() + " with " + opponent.getLevel() + " points.");
}

    public boolean isPlayAgain() {
        return playAgain;
    }

    public void setPlayAgain(boolean playAgain) {
        this.playAgain = playAgain;
    }   



}