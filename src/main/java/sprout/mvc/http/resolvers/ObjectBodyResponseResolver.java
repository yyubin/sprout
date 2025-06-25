package sprout.mvc.http.resolvers;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.ResponseEntity;
import sprout.mvc.http.ResponseResolver;

@Component
public class ObjectBodyResponseResolver implements ResponseResolver {
    @Override
    public boolean supports(Object returnValue) {
        // ResponseEntity가 아니고 void나 null이 아닌 모든 객체를 처리
        return returnValue != null && !(returnValue instanceof ResponseEntity);
    }

    @Override
    public ResponseEntity<?> resolve(Object returnValue, HttpRequest request) {
        // POST 요청이었다면 201 Created 반환
        if ("POST".equalsIgnoreCase(request.getMethod().name())) {
            return ResponseEntity.created(returnValue);
        }
        // 그 외 (GET, PUT, DELETE 등)는 200 OK 반환
        return ResponseEntity.ok(returnValue);
    }
}