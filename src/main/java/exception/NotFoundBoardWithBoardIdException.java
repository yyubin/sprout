package exception;

public class NotFoundBoardWithBoardIdException extends RuntimeException {

    public NotFoundBoardWithBoardIdException() {
        super();
    }

    public NotFoundBoardWithBoardIdException(String message, Long boardId) {
        super(message + " " + boardId);
    }

    public NotFoundBoardWithBoardIdException(String message, Long boardId, Throwable cause) {
        super(message + " " + boardId, cause);
    }

    public NotFoundBoardWithBoardIdException(Throwable cause) {
        super(cause);
    }
}
