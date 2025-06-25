package sprout.mvc.http.resolvers;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.ResponseEntity;
import sprout.mvc.http.ResponseResolver;

@Component
public class VoidResponseResolver implements ResponseResolver {
    @Override
    public boolean supports(Object returnValue) {
        return returnValue == null;
    }

    @Override
    public ResponseEntity<?> resolve(Object returnValue, HttpRequest request) {
        // 본문이 없으므로 204 No Content 반환
        return ResponseEntity.noContent();
    }
}
