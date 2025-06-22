package sprout.mvc.argument.builtins;

import app.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.beans.annotation.Component;
import sprout.mvc.annotation.RequestBody;
import sprout.mvc.argument.ArgumentResolver;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.http.ResponseCode;

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
        // HttpRequest의 바디가 String 타입으로 넘어올 것을 예상
        String rawBody = (String) request.getBody();

        if (rawBody == null || rawBody.isBlank()) {
            // @RequestBody가 붙었지만 바디가 비어있는 경우, null을 반환하거나 예외를 던질 수 있음
            return null;
        }

        try {
            // String 타입의 rawBody를 직접 대상 타입으로 변환
            return objectMapper.readValue(rawBody.trim(), parameter.getType());
        } catch (Exception e) { // JsonProcessingException 등 ObjectMapper에서 발생할 수 있는 모든 예외 처리
            // BadRequestException을 던져 클라이언트에 400 Bad Request 응답
            throw new BadRequestException(
                    "Failed to parse request body as JSON or convert to '" + parameter.getType().getName() + "'. " +
                            "Check JSON format and target type. Cause: " + e.getMessage(), ResponseCode.BAD_REQUEST, e);
        }
    }
}
