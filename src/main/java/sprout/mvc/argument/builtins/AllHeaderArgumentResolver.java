package sprout.mvc.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.mvc.annotation.Header;
import sprout.mvc.argument.ArgumentResolver;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Parameter;
import java.util.Map;

@Component
public class AllHeaderArgumentResolver implements ArgumentResolver {

    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(Header.class) && parameter.getAnnotation(Header.class).value().isEmpty();
    }

    @Override
    public Object resolve(Parameter parameter, HttpRequest<?> request, Map<String, String> pathVariables) throws Exception {
        Header headerAnnotation = parameter.getAnnotation(Header.class);
        String headerName = headerAnnotation.value();
        if (parameter.getType().equals(Map.class)) {
            if (headerName.isBlank()) { // @Header("") 또는 @Header(value="") 인 경우
                return request.getHeaders();
            } else {
                // @Header("SpecificHeader") Map<String, String> 처럼 오는 경우 (사용 안 함)
                throw new IllegalArgumentException("Cannot bind specific header '" + headerName + "' to a Map parameter. Use Map<String, String> without @Header for all headers.");
            }
        }
        return null;
    }
}
