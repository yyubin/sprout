package sprout.security.authorization.exception;

import sprout.security.authentication.exception.AuthenticationException;

public class AccessDeniedException extends AuthenticationException {
    public AccessDeniedException(String message) {
        super(message);
    }

    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccessDeniedException(Throwable cause) {
        super(cause);
    }

    public AccessDeniedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public AccessDeniedException() {
    }
}
