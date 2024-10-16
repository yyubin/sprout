package exception;

import message.ExceptionMessage;

public class AlreadyLoggedInException extends RuntimeException{

    public AlreadyLoggedInException() {
        super(ExceptionMessage.ALREADY_LOGGED_IN);
    }

    public AlreadyLoggedInException(String message) {
        super(ExceptionMessage.ALREADY_LOGGED_IN);
    }

    public AlreadyLoggedInException(String message, Throwable cause) {
        super(ExceptionMessage.ALREADY_LOGGED_IN, cause);
    }

    public AlreadyLoggedInException(Throwable cause) {
        super(cause);
    }
}
