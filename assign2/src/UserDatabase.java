import java.util.HashMap;
import java.util.Map;

public class UserDatabase {
    private Map<String, String> users;

    public UserDatabase() {
        users = new HashMap<>();
        populateSampleUsers();
    }

    private void populateSampleUsers() {
        users.put("user1", "password1");
        users.put("user2", "password2");
        users.put("user3", "password3");
    }

    public boolean authenticate(String username, String password) {
        String storedPassword = users.get(username);
        return storedPassword != null && storedPassword.equals(password);
    }
}