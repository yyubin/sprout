package app.exception;

public class NotFoundComment extends RuntimeException {
    public NotFoundComment() {
        super();
    }

    public NotFoundComment(String message) {
        super(message);
    }

    public NotFoundComment(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundComment(Throwable cause) {
        super(cause);
    }
}
