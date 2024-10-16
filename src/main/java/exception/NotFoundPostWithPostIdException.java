package exception;

public class NotFoundPostWithPostIdException extends RuntimeException {

    public NotFoundPostWithPostIdException() {
        super();
    }

    public NotFoundPostWithPostIdException(String message, Long postId) {
        super(message + " " + postId);
    }

    public NotFoundPostWithPostIdException(String message, Long postId, Throwable cause) {
        super(message + " " + postId, cause);
    }

    public NotFoundPostWithPostIdException(Throwable cause) {
        super(cause);
    }
}
