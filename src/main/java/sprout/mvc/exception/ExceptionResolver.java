package sprout.mvc.exception;

import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.HttpResponse;

public interface ExceptionResolver {
    Object resolveException(HttpRequest<?> request, HttpResponse response, Object handlerMethod, Exception exception);
}
