package app.exception;

public class MemberEmailAlreadyExistsException extends RuntimeException{

    public MemberEmailAlreadyExistsException() {
        super();
    }

    public MemberEmailAlreadyExistsException(String message) {
        super(message);
    }

    public MemberEmailAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public MemberEmailAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
