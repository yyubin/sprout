package sprout.security.authorization.exception;

public class AuthorizationAspect extends RuntimeException {
    public AuthorizationAspect(String message) {
        super(message);
    }

    public AuthorizationAspect() {
      super();
    }

    public AuthorizationAspect(String message, Throwable cause) {
      super(message, cause);
    }

    public AuthorizationAspect(Throwable cause) {
      super(cause);
    }

    protected AuthorizationAspect(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
    }
}
