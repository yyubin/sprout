package sprout.mvc.mapping;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RequestMappingRegistry {
    private final Map<PathPattern, Map<HttpMethod, RequestMappingInfo>> mappings = new ConcurrentHashMap<>();
    private final Map<String, PathPattern> pathPatterns = new ConcurrentHashMap<>();

    public void register(PathPattern pathPattern, HttpMethod httpMethod, Object controller, Method handlerMethod) {
        System.out.println("Registering request mapping for " + pathPattern.getOriginalPattern() + " with http method " + httpMethod);
        mappings.computeIfAbsent(pathPattern, k -> new EnumMap<>(HttpMethod.class))
                .put(httpMethod, new RequestMappingInfo(pathPattern, httpMethod, controller, handlerMethod));
    }

    public RequestMappingInfo getHandlerMethod(String path, HttpMethod httpMethod) {
        if (pathPatterns.containsKey(path)) {
            return mappings.get(pathPatterns.get(path)).get(httpMethod);
        }

        List<RequestMappingInfo> matchingHandlers = new ArrayList<>();

        // 1. 먼저 요청 경로와 일치하는 모든 핸들러를 찾는다.
        for (PathPattern registeredPattern : mappings.keySet()) {
            if (registeredPattern.matches(path)) {
                Map<HttpMethod, RequestMappingInfo> methodMappings = mappings.get(registeredPattern);
                if (methodMappings != null && methodMappings.containsKey(httpMethod)) {
                    matchingHandlers.add(methodMappings.get(httpMethod));
                }
            }
        }
        if (matchingHandlers.isEmpty()) {
            return null;
        }
        // 2. 찾은 핸들러들을 PathPattern의 우선순위에 따라 정렬한다.
        matchingHandlers.sort(Comparator.comparing(RequestMappingInfo::pattern));

        // 3. 가장 우선순위가 높은 (가장 구체적인) 핸들러를 반환한다.

        // 문자열 경로 캐싱
        pathPatterns.put(path, matchingHandlers.getFirst().pattern());

        return matchingHandlers.get(0);
    }
}