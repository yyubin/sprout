package sprout.server.websocket.exception;

public class NotEnoughDataException extends WebSocketException {
    public NotEnoughDataException(String message) {
        super(message);
    }

    public NotEnoughDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEnoughDataException(Throwable cause) {
        super(cause);
    }

    public NotEnoughDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public NotEnoughDataException() {
    }
}
