package sprout.mvc.argument.builtins;

import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.beans.annotation.Component;
import sprout.mvc.annotation.RequestBody;
import sprout.mvc.argument.ArgumentResolver;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Parameter;
import java.util.Map;

@Component
public class RequestBodyArgumentResolver implements ArgumentResolver {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestBody.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpRequest<Map<String, Object>> request, Map<String, String> pathVariables) throws Exception {
        Map<String, Object> body = request.getBody();
        if (body == null) {
            return null;
        }

        return objectMapper.convertValue(body, parameter.getType());
    }
}
