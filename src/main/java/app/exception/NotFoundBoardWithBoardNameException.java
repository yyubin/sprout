package app.exception;

public class NotFoundBoardWithBoardNameException extends RuntimeException {

    public NotFoundBoardWithBoardNameException() {
      super();
    }

    public NotFoundBoardWithBoardNameException(String message) {
      super(message);
    }

    public NotFoundBoardWithBoardNameException(String message, Throwable cause) {
      super(message, cause);
    }

    public NotFoundBoardWithBoardNameException(Throwable cause) {
      super(cause);
    }
}
