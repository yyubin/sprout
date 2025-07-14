package sprout.security.authentication.exception;

public class ProviderNotFoundException extends LoginException {
    public ProviderNotFoundException(String message) {
        super(message);
    }

    public ProviderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderNotFoundException(Throwable cause) {
        super(cause);
    }

    public ProviderNotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ProviderNotFoundException() {
    }
}
