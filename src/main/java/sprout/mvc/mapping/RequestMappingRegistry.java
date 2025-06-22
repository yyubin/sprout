package sprout.mvc.mapping;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.*;

@Component
public class RequestMappingRegistry {
    private final Map<PathPattern, Map<HttpMethod, RequestMappingInfo>> mappings = new LinkedHashMap<>();

    public void register(PathPattern pathPattern, HttpMethod httpMethod, Object controller, Method handlerMethod) {
        System.out.println("Registering request mapping for " + pathPattern.getOriginalPattern() + " with http method " + httpMethod);
        mappings.computeIfAbsent(pathPattern, k -> new EnumMap<>(HttpMethod.class))
                .put(httpMethod, new RequestMappingInfo(pathPattern, httpMethod, controller, handlerMethod));
    }

    public RequestMappingInfo getHandlerMethod(String path, HttpMethod httpMethod) {
        List<PathPattern> sortedPatterns = new ArrayList<>(mappings.keySet());
        Collections.sort(sortedPatterns, Comparator.comparingInt(PathPattern::getVariableCount));

        for (PathPattern registeredPattern : sortedPatterns) {
            if (registeredPattern.matches(path)) { // 매칭 확인
                Map<HttpMethod, RequestMappingInfo> methodMappings = mappings.get(registeredPattern);
                if (methodMappings != null) {
                    RequestMappingInfo info = methodMappings.get(httpMethod);
                    if (info != null) {
                        System.out.println("Found handler for " + registeredPattern.getOriginalPattern());
                        return info;
                    }
                }
            }
        }
        return null;
    }
}