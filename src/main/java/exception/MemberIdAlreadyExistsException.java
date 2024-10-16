package exception;

import message.ExceptionMessage;

public class MemberIdAlreadyExistsException extends RuntimeException {

    public MemberIdAlreadyExistsException() {
        super();
    }

    public MemberIdAlreadyExistsException(String message) {
        super(ExceptionMessage.MEMBER_ID_ALREADY_EXISTS);
    }

    public MemberIdAlreadyExistsException(String message, Throwable cause) {
        super(ExceptionMessage.MEMBER_ID_ALREADY_EXISTS, cause);
    }

    public MemberIdAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
