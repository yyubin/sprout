package sprout.security.authentication;

import sprout.security.authentication.exception.LoginException;
import sprout.security.core.Authentication;

@FunctionalInterface
public interface AuthenticationManager {
    Authentication authenticate(Authentication authentication) throws LoginException;
}
