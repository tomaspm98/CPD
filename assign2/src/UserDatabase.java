import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.IOException;

public class UserDatabase {
    private Map<String, String> users;
    private Map<String,Integer> usersLevels;
    private File file;

    public UserDatabase() {
        users = new HashMap<>();
        usersLevels = new HashMap<>();
        file = new File("users.txt");
        loadUsersFromFile();
    }

    private void loadUsersFromFile() {
        try {
            //File file = new File("users.txt");
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(":");  // Split username and password by ':'
                if (parts.length == 3) {
                    String username = parts[0];
                    String password = parts[1];
                    int level = Integer.parseInt(parts[2]);
                    users.put(username, password);
                    usersLevels.put(username,level);
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
        usersLevels.put(username,0);

        return saveUsersToFile();
    }

    public boolean authenticate(String username, String password) {
        String storedPassword = users.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }

     public int getLevel(String username) {
        return usersLevels.getOrDefault(username, 0);
    }

    public void setLevel(String username, int level) {
        if (users.containsKey(username)) {
            usersLevels.put(username, level);
            saveUsersToFile();
        }
    }

    private boolean saveUsersToFile() {
        try (PrintWriter out = new PrintWriter(file)) {
            for (String username : users.keySet()) {
                String password = users.get(username);
                int level = usersLevels.get(username);
                out.println(username + ":" + password + ":" + level);
            }
            return true;
        } catch (IOException e) {
            System.err.println("Error writing to user file: " + e.getMessage());
            return false;
        }
    }
}
