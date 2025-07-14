package sprout.security.authentication;

import sprout.security.authentication.exception.LoginException;
import sprout.security.core.Authentication;


public interface AuthenticationEventPublisher {
    void publishAuthenticationSuccess(Authentication authentication);

    void publishAuthenticationFailure(LoginException exception, Authentication authentication);

}
