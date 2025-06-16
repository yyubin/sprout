package sprout.mvc.invoke;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.argument.CompositeArgumentResolver;

import java.util.Map;

@Component
public class HandlerMethodInvoker {
    private final CompositeArgumentResolver resolvers;

    public HandlerMethodInvoker(CompositeArgumentResolver resolvers) {
        this.resolvers = resolvers;
    }

    public Object invoke(HandlerMethod handlerMethod, HttpRequest<Map<String, Object>> request) throws Exception {
        Object[] args = resolvers.resolveArguments(handlerMethod.method(), request);
        return handlerMethod.method().invoke(handlerMethod.controller(), args);
    }
}
