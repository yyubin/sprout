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
    public Object resolve(Parameter parameter, HttpRequest<?> request, Map<String, String> pathVariables) throws Exception {
        var body = request.getBody(); // HttpRequest<?>의 body는 Object 타입으로 추론
        // 요청 바디 자체가 없는 경우 (null)
        if (body == null) {
            return null;
        }

        try {
            // ObjectMapper를 사용하여 바디 데이터를 대상 타입으로 변환 시도
            return objectMapper.convertValue(body, parameter.getType());
        } catch (IllegalArgumentException e) {
            // Jackson의 convertValue 메서드에서 변환 실패 시 주로 던져지는 예외
            throw new IllegalArgumentException(
                    "Failed to convert request body to '" + parameter.getType().getName() + "'. " +
                            "Check the JSON structure and target type compatibility. Cause: " + e.getMessage(), e);
        } catch (Exception e) {
            // 그 외 예상치 못한 예외 처리
            throw new RuntimeException(
                    "An unexpected error occurred during request body resolution for parameter '" +
                            parameter.getName() + "': " + e.getMessage(), e);
        }
    }
}
