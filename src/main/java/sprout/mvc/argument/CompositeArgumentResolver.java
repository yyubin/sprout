package sprout.mvc.argument;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

@Component
public class CompositeArgumentResolver {
    private final List<ArgumentResolver> delegates;

    public CompositeArgumentResolver(List<ArgumentResolver> delegates) {
        this.delegates = delegates;
    }

    public Object[] resolveArguments(Method method, HttpRequest<Map<String, Object>> request) throws Exception {
        Parameter[] params = method.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            ArgumentResolver resolver = delegates.stream()
                    .filter(ar -> ar.supports(p))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No ArgumentResolver for parameter " + p));
            args[i] = resolver.resolve(p, request);
        }
        return args;
    }
}