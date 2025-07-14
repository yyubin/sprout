package sprout.security.authentication;

import sprout.security.core.Authentication;

import javax.naming.AuthenticationException;

public interface AuthenticationProvider {
    boolean supports(Class<?> authentication);
    Authentication authenticate(Authentication authentication) throws AuthenticationException;
}
