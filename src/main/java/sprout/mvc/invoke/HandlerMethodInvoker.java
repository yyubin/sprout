package sprout.mvc.invoke;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.argument.CompositeArgumentResolver;
import sprout.mvc.mapping.PathPattern;
import sprout.mvc.mapping.RequestMappingInfo;

import java.util.Map;

@Component
public class HandlerMethodInvoker {
    private final CompositeArgumentResolver resolvers;

    public HandlerMethodInvoker(CompositeArgumentResolver resolvers) {
        this.resolvers = resolvers;
    }

    public Object invoke(RequestMappingInfo<?> requestMappingInfo, HttpRequest<?> request) throws Exception {
        PathPattern pattern = requestMappingInfo.pattern();
        Map<String, String> pathVariables = pattern.extractPathVariables(request.getPath());

        Object[] args = resolvers.resolveArguments(requestMappingInfo.handlerMethod(), request, pathVariables);
        return requestMappingInfo.handlerMethod().invoke(requestMappingInfo.controller(), args);
    }
}
