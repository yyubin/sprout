package sprout.mvc.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.mvc.annotation.RequestParam;
import sprout.mvc.argument.ArgumentResolver;
import sprout.mvc.argument.TypeConverter;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Parameter;
import java.util.Map;

@Component
public class RequestParamArgumentResolver implements ArgumentResolver {
    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(RequestParam.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpRequest<?> request, Map<String, String> pathVariables) throws Exception {
        RequestParam requestParam = parameter.getAnnotation(RequestParam.class);
        String paramName = requestParam.value().isEmpty() ? parameter.getName() : requestParam.value();
        String paramValue = request.getQueryParams().get(paramName);

        if (paramValue == null) {
            if (requestParam.required()) {
                throw new IllegalArgumentException("Required request parameter '" + paramName + "' not found in request.");
            }
        }

        return TypeConverter.convert(paramValue, parameter.getType());
    }

}
