package sprout.server.websocket.exception;

public class WebSocketProtocolException extends WebSocketException {
    public WebSocketProtocolException(String message) {
        super(message);
    }

    public WebSocketProtocolException(String message, Throwable cause) {
        super(message, cause);
    }

    public WebSocketProtocolException(Throwable cause) {
        super(cause);
    }

    public WebSocketProtocolException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public WebSocketProtocolException() {
        super();
    }
}
