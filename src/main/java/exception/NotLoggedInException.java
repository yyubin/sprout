package exception;

import message.ExceptionMessage;

public class NotLoggedInException extends RuntimeException{

    public NotLoggedInException() {
        super(ExceptionMessage.NOT_LOGGED_IN);
    }

    public NotLoggedInException(String message) {
        super(ExceptionMessage.NOT_LOGGED_IN);
    }

    public NotLoggedInException(String message, Throwable cause) {
        super(ExceptionMessage.ALREADY_LOGGED_IN, cause);
    }

    public NotLoggedInException(Throwable cause) {
        super(cause);
    }
}
