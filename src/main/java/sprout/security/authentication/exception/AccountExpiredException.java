package sprout.security.authentication.exception;

public class AccountExpiredException extends LoginException {
    public AccountExpiredException(String message) {
        super(message);
    }
}
