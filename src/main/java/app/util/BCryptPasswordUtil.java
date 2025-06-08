package app.util;

import org.mindrot.jbcrypt.BCrypt;
import app.util.interfaces.PasswordUtil;

public class BCryptPasswordUtil implements PasswordUtil {

    public static String encryptPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    public static boolean matchPassword(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}
