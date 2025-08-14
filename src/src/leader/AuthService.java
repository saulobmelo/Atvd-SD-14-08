package leader;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private final Set<String> valid = ConcurrentHashMap.newKeySet();
    private final SecureRandom rnd = new SecureRandom();

    public String login(String user, String pass) {
        if (user == null || user.isBlank() || pass == null || pass.isBlank()) return null;
        byte[] buf = new byte[24];
        rnd.nextBytes(buf);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        valid.add(token);
        return token;
    }

    public boolean isValid(String token) {
        return token != null && valid.contains(token);
    }
}