package util;

public interface PasswordUtil {
    String encryptPassword(String rawPassword);

    boolean matchPassword(String rawPassword, String hashedPassword);
}
