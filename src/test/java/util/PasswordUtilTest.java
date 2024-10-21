package util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {

    @Test
    public void testEncryptPassword() {
        String password = "password";
        String encryptedPassword = BCryptPasswordUtil.encryptPassword(password);
        assertTrue(BCryptPasswordUtil.matchPassword(password, encryptedPassword));
    }
}
