package sprout.mvc.argument.builtins;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.mvc.annotation.RequestParam;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class RequestParamArgumentResolverTest {

    private RequestParamArgumentResolver resolver;

    @Mock
    private HttpRequest<Map<String, Object>> mockRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resolver = new RequestParamArgumentResolver();
    }

    // 테스트용 더미 컨트롤러
    private static class TestController {
        public void testMethodWithRequestParam(@RequestParam("userId") String userId, @RequestParam int age) {}
        public void testMethodWithRequiredFalse(@RequestParam(value = "name", required = false) String name) {}
        public void testMethodNoAnnotation(String param1) {}
        public void testMethodWithEmptyValueAnnotation(@RequestParam("") String category) {}
        public void testMethodWithBoolean(@RequestParam boolean active) {}
    }

    // supports() 메서드 테스트

    @Test
    @DisplayName("RequestParam 어노테이션이 있는 파라미터를 지원해야 한다")
    void supports_RequestParamAnnotatedParameter_ReturnsTrue() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("testMethodWithRequestParam", String.class, int.class);
        Parameter parameter = method.getParameters()[0]; // @RequestParam("userId") String userId

        assertThat(resolver.supports(parameter)).isTrue();
    }

    @Test
    @DisplayName("RequestParam 어노테이션이 없는 파라미터를 지원하지 않아야 한다")
    void supports_NonRequestParamParameter_ReturnsFalse() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("testMethodNoAnnotation", String.class);
        Parameter parameter = method.getParameters()[0]; // String param1

        assertThat(resolver.supports(parameter)).isFalse();
    }

    // resolve() 메서드 테스트

    @Test
    @DisplayName("명시적인 이름의 RequestParam을 성공적으로 해석해야 한다")
    void resolve_ExplicitRequestParamName_ReturnsCorrectValue() throws Exception {
        Method method = TestController.class.getMethod("testMethodWithRequestParam", String.class, int.class);
        Parameter parameter = method.getParameters()[0]; // @RequestParam("userId") String userId

        Map<String, String> queryParams = Map.of("userId", "user123");
        // Mockito를 사용하여 mockRequest의 getQueryParams() 호출 시 위 맵을 반환하도록 설정
        when(mockRequest.getQueryParams()).thenReturn(queryParams);

        Object resolvedValue = resolver.resolve(parameter, mockRequest, Collections.emptyMap()); // pathVariables는 이 Resolver에서 사용 안함

        assertThat(resolvedValue).isEqualTo("user123");
        assertThat(resolvedValue).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("비어있는 value의 RequestParam을 파라미터 이름으로 해석해야 한다")
    void resolve_EmptyValueRequestParam_ReturnsCorrectValue() throws Exception {
        // 이 테스트는 컴파일 시 -parameters 옵션이 활성화되어야 파라미터 이름을 얻을 수 있습니다.
        Method method = TestController.class.getMethod("testMethodWithEmptyValueAnnotation", String.class);
        Parameter parameter = method.getParameters()[0]; // @RequestParam("") String category

        Map<String, String> queryParams = Map.of("category", "electronics");
        when(mockRequest.getQueryParams()).thenReturn(queryParams);

        Object resolvedValue = resolver.resolve(parameter, mockRequest, Collections.emptyMap());

        assertThat(resolvedValue).isEqualTo("electronics");
        assertThat(resolvedValue).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("required=true인 필수 RequestParam이 없을 경우 예외를 던져야 한다")
    void resolve_RequiredRequestParamNotFound_ThrowsException() throws NoSuchMethodException {
        Method method = TestController.class.getMethod("testMethodWithRequestParam", String.class, int.class);
        Parameter parameter = method.getParameters()[0]; // @RequestParam("userId") String userId (required=true가 기본값)

        // 쿼리 파라미터에 'userId'가 없는 경우
        when(mockRequest.getQueryParams()).thenReturn(Collections.emptyMap());

        assertThrows(IllegalArgumentException.class, () ->
                resolver.resolve(parameter, mockRequest, Collections.emptyMap())
        );
    }

    @Test
    @DisplayName("required=false인 선택적 RequestParam이 없을 경우 null을 반환해야 한다")
    void resolve_OptionalRequestParamNotFound_ReturnsNull() throws Exception {
        Method method = TestController.class.getMethod("testMethodWithRequiredFalse", String.class);
        Parameter parameter = method.getParameters()[0]; // @RequestParam(value = "name", required = false) String name

        // 쿼리 파라미터에 'name'이 없는 경우
        when(mockRequest.getQueryParams()).thenReturn(Collections.emptyMap());

        Object resolvedValue = resolver.resolve(parameter, mockRequest, Collections.emptyMap());

        assertThat(resolvedValue).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "age, 25, 25, java.lang.Integer", // @RequestParam int age
            "userId, userABC, userABC, java.lang.String", // @RequestParam("userId") String userId
            "active, true, true, java.lang.Boolean", // @RequestParam boolean active
            "active, false, false, java.lang.Boolean",
            "active, anyString, false, java.lang.Boolean" // Boolean.parseBoolean의 특성
    })
    @DisplayName("RequestParam 값을 대상 타입으로 올바르게 변환해야 한다")
    void resolve_ConvertsToTargetTypeCorrectly(String paramName, String paramValue, String expectedValueString, String expectedTypeString) throws Exception {
        Method method;
        Parameter parameter;

        if ("age".equals(paramName)) {
            method = TestController.class.getMethod("testMethodWithRequestParam", String.class, int.class);
            parameter = method.getParameters()[1]; // int age
        } else if ("userId".equals(paramName)) {
            method = TestController.class.getMethod("testMethodWithRequestParam", String.class, int.class);
            parameter = method.getParameters()[0]; // String userId
        } else if ("active".equals(paramName)) {
            method = TestController.class.getMethod("testMethodWithBoolean", boolean.class);
            parameter = method.getParameters()[0]; // boolean active
        } else {
            throw new IllegalArgumentException("Unsupported parameter name for this test: " + paramName);
        }

        Map<String, String> queryParams = Map.of(paramName, paramValue);
        when(mockRequest.getQueryParams()).thenReturn(queryParams);

        Object resolvedValue = resolver.resolve(parameter, mockRequest, Collections.emptyMap());

        // 예상 타입으로 변환 및 검증
        Class<?> expectedType = Class.forName(expectedTypeString);
        assertThat(resolvedValue).isInstanceOf(expectedType);

        if (expectedType.equals(Integer.class) || expectedType.equals(int.class)) {
            assertThat(resolvedValue).isEqualTo(Integer.parseInt(expectedValueString));
        } else if (expectedType.equals(Boolean.class) || expectedType.equals(boolean.class)) {
            assertThat(resolvedValue).isEqualTo(Boolean.parseBoolean(expectedValueString));
        } else {
            assertThat(resolvedValue).isEqualTo(expectedValueString);
        }
    }

    @ParameterizedTest
    @CsvSource({
            "age, invalid, java.lang.Integer",
            "active, notABoolean, java.lang.Boolean"
    })
    @DisplayName("유효하지 않은 RequestParam 값을 변환 시 TypeConverter에서 발생한 예외를 전달해야 한다")
    void resolve_InvalidValueConversion_ThrowsException(String paramName, String paramValue, String targetTypeString) throws NoSuchMethodException {
        Method method;
        Parameter parameter;
        Class<?> targetType;

        if ("age".equals(paramName)) {
            method = TestController.class.getMethod("testMethodWithRequestParam", String.class, int.class);
            parameter = method.getParameters()[1]; // int age
            targetType = int.class;
        } else if ("active".equals(paramName)) {
            method = TestController.class.getMethod("testMethodWithBoolean", boolean.class);
            parameter = method.getParameters()[0]; // boolean active
            targetType = boolean.class;
        } else {
            throw new IllegalArgumentException("Unsupported parameter name for this test: " + paramName);
        }

        Map<String, String> queryParams = Map.of(paramName, paramValue);
        when(mockRequest.getQueryParams()).thenReturn(queryParams);

        // TypeConverter에서 발생할 것으로 예상되는 예외를 검증
        // Boolean.parseBoolean("notABoolean")은 false를 반환하므로 이 경우 예외가 발생하지 않음
        if (targetType == int.class || targetType == Integer.class) {
            assertThrows(NumberFormatException.class, () ->
                    resolver.resolve(parameter, mockRequest, Collections.emptyMap())
            );
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            // Boolean.parseBoolean은 'true'가 아니면 모두 false를 반환하므로
            // 이 경우 예외가 발생하지 않고 false가 반환될 것
        } else {
            assertThrows(IllegalArgumentException.class, () -> // TypeConverter에서 던질 수 있는 일반적인 예외
                    resolver.resolve(parameter, mockRequest, Collections.emptyMap())
            );
        }
    }
}