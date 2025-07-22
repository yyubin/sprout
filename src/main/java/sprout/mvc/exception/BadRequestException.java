package sprout.mvc.exception;

import sprout.mvc.http.ResponseCode;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(String message, ResponseCode responseCode) {
        super(message);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }

    public BadRequestException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BadRequestException() {
    }

    public BadRequestException(String s, ResponseCode responseCode, Exception e) {
        super(s, e);
    }
}
