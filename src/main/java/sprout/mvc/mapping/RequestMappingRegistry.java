package sprout.mvc.mapping;

import sprout.mvc.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class RequestMappingRegistry {
    private final Map<String, Map<HttpMethod, RequestMappingInfo>> mappings = new HashMap<>();

    public void register(String path, HttpMethod httpMethod, ControllerInterface controller, Method handlerMethod) {
        mappings.computeIfAbsent(path, k -> new EnumMap<>(HttpMethod.class))
                .put(httpMethod, new RequestMappingInfo(path, httpMethod, controller, handlerMethod));
    }

    public RequestMappingInfo getHandlerMethod(String path, HttpMethod httpMethod) {
        Map<HttpMethod, RequestMappingInfo> methodMappings = mappings.get(path);
        if (methodMappings != null) {
            return methodMappings.get(httpMethod);
        }
        return null;
    }
}