package sprout.mvc.http.resolvers;

import sprout.beans.annotation.Component;
import sprout.beans.annotation.Order;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.ResponseEntity;
import sprout.mvc.http.ResponseEntityBuilder;
import sprout.mvc.http.ResponseResolver;

@Component
@Order(1)
public class StringResponseResolver implements ResponseResolver {
    @Override
    public boolean supports(Object returnValue) {
        return returnValue instanceof String;
    }

    @Override
    public ResponseEntity<?> resolve(Object returnValue, HttpRequest request) {
        return ResponseEntityBuilder
                .ok()
                .contentType("text/plain")
                .build();
    }
}
