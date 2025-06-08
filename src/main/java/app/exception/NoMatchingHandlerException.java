package app.exception;

public class NoMatchingHandlerException extends RuntimeException {
    public NoMatchingHandlerException() {
        super();
    }

    public NoMatchingHandlerException(String message) {
        super(message);
    }

    public NoMatchingHandlerException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoMatchingHandlerException(Throwable cause) {
        super(cause);
    }
}
