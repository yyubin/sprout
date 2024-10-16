package util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    public void testEncryptPassword() {
        String password = "password";
        String encryptedPassword = PasswordUtil.encryptPassword(password);
        assertTrue(PasswordUtil.matchPassword(password, encryptedPassword));
    }
}
