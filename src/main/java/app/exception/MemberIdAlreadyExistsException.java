package app.exception;

public class MemberIdAlreadyExistsException extends RuntimeException {

    public MemberIdAlreadyExistsException() {
        super();
    }

    public MemberIdAlreadyExistsException(String message) {
        super(message);
    }

    public MemberIdAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public MemberIdAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
