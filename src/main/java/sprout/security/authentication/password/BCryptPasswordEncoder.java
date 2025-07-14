package sprout.security.authentication.password;

import org.mindrot.jbcrypt.BCrypt;
import sprout.beans.InfrastructureBean;

public class BCryptPasswordEncoder implements PasswordEncoder, InfrastructureBean {
    @Override
    public String encode(CharSequence rawPassword) {
        return BCrypt.hashpw(rawPassword.toString(), BCrypt.gensalt());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword.toString(), encodedPassword);
    }
}
