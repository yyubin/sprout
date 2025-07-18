package sprout.server.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.mvc.annotation.PathVariable;
import sprout.mvc.argument.TypeConverter;
import sprout.server.websocket.InvocationContext;
import sprout.server.argument.WebSocketArgumentResolver;

import java.lang.reflect.Parameter;

@Component
public class PathPathVariableArgumentResolver implements WebSocketArgumentResolver {
    @Override
    public boolean supports(Parameter parameter, InvocationContext context) {
        return parameter.isAnnotationPresent(PathVariable.class) && context.pathVars() != null;
    }

    @Override
    public Object resolve(Parameter parameter, InvocationContext context) throws Exception {
        PathVariable annotation = parameter.getAnnotation(PathVariable.class);
        String varName = annotation.value().isEmpty() ? parameter.getName() : annotation.value();

        String value = context.pathVars().get(varName);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Path variable '" + varName + "' not found.");
        }
        return TypeConverter.convert(value, parameter.getType());
    }
}
