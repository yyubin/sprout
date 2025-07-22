package sprout.mvc.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.mvc.annotation.Header;
import sprout.mvc.argument.ArgumentResolver;
import sprout.mvc.argument.TypeConverter;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Parameter;
import java.util.Map;

@Component
public class HeaderArgumentResolver implements ArgumentResolver {
    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(Header.class) && !parameter.getAnnotation(Header.class).value().isEmpty();
    }

    @Override
    public Object resolve(Parameter parameter, HttpRequest<?> request, Map<String, String> pathVariables) throws Exception {
        Header headerAnnotation = parameter.getAnnotation(Header.class);
        if (headerAnnotation == null) {
            return null;
        }
        String headerName = headerAnnotation.value();

        if (headerName.isBlank()) {
            headerName = parameter.getName();
        }

        String headerValue = request.getHeaders().get(headerName);

        return TypeConverter.convert(headerValue, parameter.getType());
    }
}
