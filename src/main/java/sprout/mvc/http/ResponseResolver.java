package sprout.mvc.http;

public interface ResponseResolver {
    boolean supports(Object returnValue);
    ResponseEntity<?> resolve(Object returnValue, HttpRequest request);
}
