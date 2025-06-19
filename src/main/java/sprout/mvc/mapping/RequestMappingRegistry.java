package sprout.mvc.mapping;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Component
public class RequestMappingRegistry {
    private final Map<String, Map<HttpMethod, RequestMappingInfo<?>>> mappings = new HashMap<>();

    public void register(String path, HttpMethod httpMethod, Object controller, Method handlerMethod) {
        System.out.println("Registering request mapping for " + path + " with http method " + httpMethod);
        mappings.computeIfAbsent(path, k -> new EnumMap<>(HttpMethod.class))
                .put(httpMethod, new RequestMappingInfo<>(path, httpMethod, controller, handlerMethod));
    }

    public RequestMappingInfo<?> getHandlerMethod(String path, HttpMethod httpMethod) {
        Map<HttpMethod, RequestMappingInfo<?>> methodMappings = mappings.get(path);
        System.out.println("path " + path + " method " + httpMethod);
        if (methodMappings != null) {
            return methodMappings.get(httpMethod);
        }
        return null;
    }
}