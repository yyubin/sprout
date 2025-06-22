package sprout.mvc.mapping;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.invoke.HandlerMethod;

@Component
public class HandlerMappingImpl implements HandlerMapping {

    private final RequestMappingRegistry registry;

    public HandlerMappingImpl(RequestMappingRegistry registry) {
        this.registry = registry;
    }

    @Override
    public HandlerMethod findHandler(String path, HttpMethod httpMethod) {
        var info = registry.getHandlerMethod(path, httpMethod);
        if (info == null) return null;
        return new HandlerMethod(info);
    }
}
