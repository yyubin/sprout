package sprout.mvc.advice;

import sprout.mvc.http.HttpRequest;

public interface ResponseAdvice {
    boolean supports(Object body, HttpRequest<?> request);
    Object beforeBodyWrite(Object body, HttpRequest<?> request);
}
