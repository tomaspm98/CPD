import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.IOException;

public class UserDatabase {
    private Map<String, String> users;
    private File file;

    public UserDatabase() {
        users = new HashMap<>();
        file = new File("users.txt");
        loadUsersFromFile();
    }

    private void loadUsersFromFile() {
        try {
            File file = new File("users.txt");
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(":");  // Split username and password by ':'
                if (parts.length == 2) {
                    String username = parts[0];
                    String password = parts[1];
                    users.put(username, password);
                }
            }

            scanner.close();
        } catch (FileNotFoundException e) {
            System.err.println("User file not found: " + e.getMessage());
        }
    }

    public boolean register(String username, String password) {
        if (users.containsKey(username)) {
            return false;  // Username already exists
        }

        users.put(username, password);

        try (PrintWriter out = new PrintWriter(file)) {
            for (Map.Entry<String, String> entry : users.entrySet()) {
                out.println(entry.getKey() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            System.err.println("Error writing to user file: " + e.getMessage());
            return false;
        }

        return true;
    }

    public boolean authenticate(String username, String password) {
        String storedPassword = users.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }
}
