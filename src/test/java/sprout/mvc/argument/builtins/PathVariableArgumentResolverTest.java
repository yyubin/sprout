package sprout.mvc.argument.builtins;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.mvc.annotation.PathVariable;
import sprout.mvc.http.HttpRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PathVariableArgumentResolverTest {

    private PathVariableArgumentResolver resolver;

    @Mock
    private HttpRequest<Map<String, Object>> mockRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resolver = new PathVariableArgumentResolver();
    }

    @Test
    @DisplayName("should support parameters annotated with PathVariable")
    void supports_PathVariableAnnotatedParameter_ReturnsTrue() throws NoSuchMethodException {
        // testMethodWithAnnotation(@PathVariable("id") String id, @PathVariable("count") int count)
        Method method = TestController.class.getMethod("testMethodWithAnnotation", String.class, int.class);
        Parameter parameter = method.getParameters()[0]; // @PathVariable("id") String id

        boolean supported = resolver.supports(parameter);

        assertThat(supported).isTrue();
    }

    @Test
    @DisplayName("should not support parameters without PathVariable annotation")
    void supports_NonPathVariableParameter_ReturnsFalse() throws NoSuchMethodException {
        // testMethodNoAnnotation(String name, int value)
        Method method = TestController.class.getMethod("testMethodNoAnnotation", String.class, int.class);
        Parameter parameter = method.getParameters()[0]; // String name

        boolean supported = resolver.supports(parameter);

        assertThat(supported).isFalse();
    }

    @Test
    @DisplayName("should resolve path variable with explicit name")
    void resolve_ExplicitPathVariableName_ReturnsCorrectValue() throws Exception {
        // testMethodWithAnnotation(@PathVariable("id") String id, @PathVariable("count") int count)
        Method method = TestController.class.getMethod("testMethodWithAnnotation", String.class, int.class);
        Parameter parameter = method.getParameters()[0]; // @PathVariable("id") String id
        Map<String, String> pathVariables = Map.of("id", "123");

        Object resolvedValue = resolver.resolve(parameter, mockRequest, pathVariables);

        assertThat(resolvedValue).isEqualTo("123");
        assertThat(resolvedValue).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("should resolve path variable using parameter name when value is empty")
    void resolve_EmptyPathVariableName_ReturnsCorrectValue() throws Exception {
        // testMethodWithEmptyAnnotation(@PathVariable("") String userId)
        Method method = TestController.class.getMethod("testMethodWithEmptyAnnotation", String.class); // <-- String.class 단일 파라미터로 수정
        Parameter parameter = method.getParameters()[0]; // @PathVariable("") String userId
        Map<String, String> pathVariables = Map.of("userId", "user456");

        Object resolvedValue = resolver.resolve(parameter, mockRequest, pathVariables);

        assertThat(resolvedValue).isEqualTo("user456");
        assertThat(resolvedValue).isInstanceOf(String.class);
    }

    @ParameterizedTest
    @CsvSource({
            "testMethodWithAnnotationInt, 1, id, 789, java.lang.Integer, 789",
            "testMethodWithAnnotation, 0, id, testName, java.lang.String, testName"
    })
    @DisplayName("should convert path variable to target type")
    void resolve_ConvertPathVariableToTargetType(String methodName, int parameterIndex, String pathVarName, String pathVarValue, String expectedType, String expectedValue) throws Exception {
        Method method;
        Parameter parameter;

        if ("testMethodWithAnnotationInt".equals(methodName)) {
            // testMethodWithAnnotationInt(@PathVariable("name") String name, @PathVariable("id") int id)
            method = TestController.class.getMethod(methodName, String.class, int.class);
            parameter = method.getParameters()[parameterIndex];
        } else if ("testMethodWithAnnotation".equals(methodName)) {
            // testMethodWithAnnotation(@PathVariable("id") String id, @PathVariable("count") int count)
            method = TestController.class.getMethod(methodName, String.class, int.class);
            parameter = method.getParameters()[parameterIndex];
        } else {
            throw new IllegalArgumentException("Unknown method name for parameterized test: " + methodName);
        }

        Map<String, String> pathVariables = new HashMap<>();
        pathVariables.put(pathVarName, pathVarValue);

        Object resolvedValue = resolver.resolve(parameter, mockRequest, pathVariables);

        assertThat(resolvedValue).isNotNull();
        assertThat(resolvedValue.getClass().getName()).isEqualTo(expectedType);
        if (resolvedValue instanceof Integer) {
            assertThat(resolvedValue).isEqualTo(Integer.parseInt(expectedValue));
        } else {
            assertThat(resolvedValue).isEqualTo(expectedValue);
        }
    }


    @Test
    @DisplayName("should throw IllegalArgumentException if path variable not found")
    void resolve_PathVariableNotFound_ThrowsException() throws NoSuchMethodException {
        // testMethodWithAnnotation(@PathVariable("id") String id, @PathVariable("count") int count)
        Method method = TestController.class.getMethod("testMethodWithAnnotation", String.class, int.class);
        Parameter parameter = method.getParameters()[0]; // @PathVariable("id") String id
        Map<String, String> pathVariables = Collections.emptyMap(); // "id"가 없는 맵

        assertThrows(IllegalArgumentException.class, () ->
                resolver.resolve(parameter, mockRequest, pathVariables)
        );
    }

    private static class TestController {
        public void testMethodWithAnnotation(@PathVariable("id") String id, @PathVariable("count") int count) {
        }

        public void testMethodNoAnnotation(String name, int value) {
        }

        public void testMethodWithEmptyAnnotation(@PathVariable("") String userId) {
        }

        public void testMethodWithAnnotationInt(@PathVariable("name") String name, @PathVariable("id") int id) {
        }
    }
}