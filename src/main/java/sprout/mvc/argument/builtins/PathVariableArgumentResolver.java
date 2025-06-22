package sprout.mvc.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.mvc.annotation.PathVariable;
import sprout.mvc.argument.ArgumentResolver;
import sprout.mvc.argument.TypeConverter;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Parameter;
import java.util.Map;

@Component
public class PathVariableArgumentResolver implements ArgumentResolver {
    @Override
    public boolean supports(Parameter parameter) {
        return parameter.isAnnotationPresent(PathVariable.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpRequest<?> request, Map<String, String> pathVariables) throws Exception {
        PathVariable pathVariableAnnotation = parameter.getAnnotation(PathVariable.class);
        String variableName = pathVariableAnnotation.value();

        if (variableName.isEmpty()) {
            variableName = parameter.getName();
        }

        String value = pathVariables.get(variableName);
        if (value == null) {
            throw new IllegalArgumentException("Path variable '" + variableName + "' not found in path.");
        }
        return TypeConverter.convert(value, parameter.getType());
    }
}
