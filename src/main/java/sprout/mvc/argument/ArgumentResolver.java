package sprout.mvc.argument;

import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Parameter;
import java.util.Map;

public interface ArgumentResolver {
    boolean supports(Parameter parameter);
    Object resolve(Parameter parameter,
                   HttpRequest<Map<String,Object>> request,
                   Map<String, String> pathVariables) throws Exception;
}
