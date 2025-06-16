package sprout.mvc.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.mvc.argument.ArgumentResolver;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Parameter;
import java.util.Map;

@Component
public class BodyResolver implements ArgumentResolver {
    @Override
    public boolean supports(Parameter parameter) {
        return parameter.getType().equals(Map.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpRequest<Map<String, Object>> request) throws Exception {
        return request.getBody();
    }
}
