package sprout.mvc.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.mvc.argument.ArgumentResolver;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Parameter;
import java.util.Map;

@Component
public class PrimitiveResolver implements ArgumentResolver {
    @Override
    public boolean supports(Parameter parameter) {
        Class<?> type = parameter.getType();
        return type.isPrimitive() || type.equals(String.class) ||
                type.equals(Integer.class) || type.equals(Long.class) ||
                type.equals(Boolean.class) || type.equals(Double.class) ||
                type.equals(Float.class) || type.equals(Character.class) ||
                type.equals(Short.class) || type.equals(Byte.class);
    }

    @Override
    public Object resolve(Parameter parameter, HttpRequest<Map<String, Object>> request) throws Exception {
        String paramName = parameter.getName();
        Object value = request.getQueryParams().get(paramName);

        // 쿼리 파라미터에 없으면 경로 변수(path variables) 등 다른 곳에서 찾도록 확장가능
//        if (value == null) {
//            value = request.getPathVariables().get(paramName); // 가상의 getPathVariables() 메서드 사용
//        }

        if (value == null) {
            if (parameter.getType().isPrimitive()) {
                if (parameter.getType() == int.class) return 0;
                if (parameter.getType() == long.class) return 0L;
                if (parameter.getType() == boolean.class) return false;
                if (parameter.getType() == double.class) return 0.0;
                if (parameter.getType() == float.class) return 0.0f;
                if (parameter.getType() == char.class) return '\u0000';
                if (parameter.getType() == short.class) return (short) 0;
                if (parameter.getType() == byte.class) return (byte) 0;
            }
            return null;
        }

        if (parameter.getType().equals(Integer.class) || parameter.getType().equals(int.class)) {
            return Integer.parseInt(value.toString());
        } else if (parameter.getType().equals(String.class)) {
            return value.toString();
        }

        return value;
    }
}
