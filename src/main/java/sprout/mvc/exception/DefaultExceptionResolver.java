package sprout.mvc.exception;

import app.exception.BadRequestException;
import sprout.beans.annotation.Component;
import sprout.beans.annotation.Order;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;
import sprout.mvc.http.ResponseCode;
import sprout.mvc.http.ResponseEntity;
import sprout.security.authentication.exception.LoginException;
import sprout.security.authorization.exception.AccessDeniedException;

@Component
@Order(Integer.MAX_VALUE-1)
public class DefaultExceptionResolver implements ExceptionResolver{
    @Override
    public Object resolveException(HttpRequest<?> request, HttpResponse response, Object handlerMethod, Exception exception) {
        if (exception instanceof BadRequestException) {
            response.setResponseEntity(new ResponseEntity<>("Bad Request: " + exception.getMessage(), null, ResponseCode.BAD_REQUEST));
        } else if (exception instanceof AccessDeniedException) {
            response.setResponseEntity(new ResponseEntity<>("Access Denied: " + exception.getMessage(), null, ResponseCode.FORBIDDEN));
        } else if (exception instanceof LoginException) {
            response.setResponseEntity(new ResponseEntity<>("Unauthorized: " + exception.getMessage(), null, ResponseCode.UNAUTHORIZED));
        } else {
            response.setResponseEntity(new ResponseEntity<>("Internal Server Error: " + exception.getMessage(), null, ResponseCode.INTERNAL_SERVER_ERROR));
        }
        // 예외 처리 완료를 의미하는 객체 반환 (null이 아님)
        return new Object();
    }
}
