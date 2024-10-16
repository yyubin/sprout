package exception;

import message.ExceptionMessage;

public class MemberEmailAlreadyExistsException extends RuntimeException{

    public MemberEmailAlreadyExistsException() {
        super();
    }

    public MemberEmailAlreadyExistsException(String message) {
        super(ExceptionMessage.MEMBER_EMAIL_ALREADY_EXISTS);
    }

    public MemberEmailAlreadyExistsException(String message, Throwable cause) {
        super(ExceptionMessage.MEMBER_EMAIL_ALREADY_EXISTS, cause);
    }

    public MemberEmailAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
