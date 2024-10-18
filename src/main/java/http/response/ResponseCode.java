package http.response;

import message.ResponseMessage;

public enum ResponseCode {
    SUCCESS(200, ResponseMessage.SUCCESS),
    NOT_FOUND(404, ResponseMessage.NOT_FOUND),
    BAD_REQUEST(400, ResponseMessage.BAD_REQUEST),
    UNAUTHORIZED(401, ResponseMessage.UNAUTHORIZED),
    FORBIDDEN(403, ResponseMessage.FORBIDDEN),
    INTERNAL_SERVER_ERROR(500, ResponseMessage.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
