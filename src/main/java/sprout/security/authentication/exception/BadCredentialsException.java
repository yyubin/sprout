package sprout.security.authentication.exception;

public class BadCredentialsException extends AuthenticationException {
    public BadCredentialsException(String message) {
        super(message);
    }

  public BadCredentialsException(String message, Throwable cause) {
    super(message, cause);
  }

  public BadCredentialsException(Throwable cause) {
    super(cause);
  }

  public BadCredentialsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public BadCredentialsException() {
  }
}
