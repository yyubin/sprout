package exception;

import message.ExceptionMessage;

public class AlreadyLoggedInException extends RuntimeException{

    public AlreadyLoggedInException() {
        super();
    }

    public AlreadyLoggedInException(String message) {
        super(message);
    }

    public AlreadyLoggedInException(String message, Throwable cause) {
        super(message, cause);
    }

    public AlreadyLoggedInException(Throwable cause) {
        super(cause);
    }
}
