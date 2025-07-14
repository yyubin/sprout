package sprout.security.authentication.exception;

public class CredentialExpiredException extends LoginException {
    public CredentialExpiredException(String message) {
        super(message);
    }
}
