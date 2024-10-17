package exception;

import http.response.ResponseCode;

public class BadRequestException extends RuntimeException {

  public BadRequestException() {
    super();
  }

  public BadRequestException(String message, ResponseCode responseCode) {
    super(responseCode.name() + " " + message);
  }

  public BadRequestException(String message, Throwable cause) {
    super(message, cause);
  }

  public BadRequestException(Throwable cause) {
    super(cause);
  }
}
