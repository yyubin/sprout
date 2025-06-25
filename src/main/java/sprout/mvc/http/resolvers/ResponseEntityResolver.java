package sprout.mvc.http.resolvers;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.ResponseEntity;
import sprout.mvc.http.ResponseResolver;

@Component
public class ResponseEntityResolver implements ResponseResolver {
    @Override
    public boolean supports(Object returnValue) {
        return returnValue instanceof ResponseEntity;
    }

    @Override
    public ResponseEntity<?> resolve(Object returnValue, HttpRequest request) {
        return (ResponseEntity<?>) returnValue;
    }
}