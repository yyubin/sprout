package exception;

public class BoardNameAlreadyExistsException extends RuntimeException{
    public BoardNameAlreadyExistsException() {
        super();
    }

    public BoardNameAlreadyExistsException(String message) {
        super(message);
    }

    public BoardNameAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public BoardNameAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
