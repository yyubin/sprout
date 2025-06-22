package sprout.mvc.invoke;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.mvc.argument.CompositeArgumentResolver;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.http.HttpRequest;
import sprout.mvc.mapping.PathPattern;
import sprout.mvc.mapping.RequestMappingInfo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HandlerMethodInvokerTest {

    private HandlerMethodInvoker handlerMethodInvoker;

    @Mock
    private CompositeArgumentResolver mockResolvers;
    @Mock
    private HttpRequest<String> mockRequest; // HttpRequest는 String 바디를 가질 것으로 가정

    // 테스트용 더미 컨트롤러 및 메서드
    static class TestController {
        public String handleNoArgs() {
            return "no_args_result";
        }

        public String handleWithParams(String param1, int param2) {
            return "param_result_" + param1 + "_" + param2;
        }

        public String handleWithPathVariable(String id) {
            return "path_var_result_" + id;
        }

        public String handleWithMultiplePathVariables(String category, String productId) {
            return "multi_path_var_result_" + category + "_" + productId;
        }

        public String handleThrowsException() {
            throw new RuntimeException("Simulated handler exception");
        }
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        handlerMethodInvoker = new HandlerMethodInvoker(mockResolvers);
    }

    @Test
    @DisplayName("인자가 없는 핸들러 메서드를 성공적으로 호출해야 한다")
    void invoke_noArgumentsHandler() throws Exception {
        // given
        TestController testController = new TestController();
        Method method = TestController.class.getMethod("handleNoArgs");

        // PathPattern은 실제 인스턴스 사용, PathPattern은 쿼리스트링 파싱하지 않음
        PathPattern pathPattern = new PathPattern("/no-args");

        // RequestMappingInfo와 HandlerMethod 레코드 사용
        RequestMappingInfo requestMappingInfo = new RequestMappingInfo(pathPattern, HttpMethod.GET, testController, method);
        HandlerMethod handlerMethod = new HandlerMethod(requestMappingInfo);

        // CompositeArgumentResolver는 인자가 없으므로 빈 배열을 반환하도록 Mocking
        when(mockResolvers.resolveArguments(eq(handlerMethod.requestMappingInfo().handlerMethod()), eq(mockRequest), any(Map.class)))
                .thenReturn(new Object[]{});

        // mockRequest.getPath()는 extractPathVariables에 사용되므로 Mocking
        when(mockRequest.getPath()).thenReturn("/no-args");

        // when
        // HandlerMethodInvoker의 invoke 인자로 RequestMappingInfo<Object>를 전달
        Object result = handlerMethodInvoker.invoke(requestMappingInfo, mockRequest);

        // then
        assertThat(result).isEqualTo("no_args_result");
        // resolveArguments가 올바른 인자로 호출되었는지 검증
        verify(mockResolvers).resolveArguments(eq(handlerMethod.requestMappingInfo().handlerMethod()), eq(mockRequest), eq(Collections.emptyMap())); // PathPattern에 변수 없으니 빈 맵
    }

    @Test
    @DisplayName("인자가 있는 핸들러 메서드를 성공적으로 호출하고 인자를 주입해야 한다")
    void invoke_withArgumentsHandler() throws Exception {
        // given
        TestController testController = new TestController();
        Method method = TestController.class.getMethod("handleWithParams", String.class, int.class);

        PathPattern pathPattern = new PathPattern("/with-params");
        RequestMappingInfo requestMappingInfo = new RequestMappingInfo(pathPattern, HttpMethod.GET, testController, method);
        HandlerMethod handlerMethod = new HandlerMethod(requestMappingInfo);

        Object[] resolvedArgs = {"testString", 123}; // ArgumentResolver가 반환할 인자들
        when(mockResolvers.resolveArguments(eq(handlerMethod.requestMappingInfo().handlerMethod()), eq(mockRequest), any(Map.class)))
                .thenReturn(resolvedArgs);
        when(mockRequest.getPath()).thenReturn("/with-params");

        // when
        Object result = handlerMethodInvoker.invoke(requestMappingInfo, mockRequest);

        // then
        assertThat(result).isEqualTo("param_result_testString_123");
        verify(mockResolvers).resolveArguments(eq(handlerMethod.requestMappingInfo().handlerMethod()), eq(mockRequest), eq(Collections.emptyMap())); // 경로 변수 없음
    }

    @Test
    @DisplayName("단일 PathVariable을 추출하고 CompositeArgumentResolver에 전달해야 한다")
    void invoke_withSinglePathVariable() throws Exception {
        // given
        TestController testController = new TestController();
        Method method = TestController.class.getMethod("handleWithPathVariable", String.class);

        PathPattern pathPattern = new PathPattern("/users/{id}");
        RequestMappingInfo requestMappingInfo = new RequestMappingInfo(pathPattern, HttpMethod.GET, testController, method);
        HandlerMethod handlerMethod = new HandlerMethod(requestMappingInfo);

        String requestPath = "/users/someId123";
        when(mockRequest.getPath()).thenReturn(requestPath); // HttpRequest의 경로 설정

        // PathPattern이 실제로 추출할 경로 변수
        Map<String, String> expectedPathVariables = new HashMap<>();
        expectedPathVariables.put("id", "someId123");

        Object[] resolvedArgs = {"someId123"}; // ArgumentResolver가 반환할 인자들 (경로 변수)
        when(mockResolvers.resolveArguments(eq(handlerMethod.requestMappingInfo().handlerMethod()), eq(mockRequest), eq(expectedPathVariables)))
                .thenReturn(resolvedArgs);

        // when
        Object result = handlerMethodInvoker.invoke(requestMappingInfo, mockRequest);

        // then
        assertThat(result).isEqualTo("path_var_result_someId123");
        // resolveArguments가 추출된 pathVariables와 함께 호출되었는지 검증
        verify(mockResolvers).resolveArguments(eq(handlerMethod.requestMappingInfo().handlerMethod()), eq(mockRequest), eq(expectedPathVariables));
    }

    @Test
    @DisplayName("여러 PathVariable을 추출하고 CompositeArgumentResolver에 전달해야 한다")
    void invoke_withMultiplePathVariables() throws Exception {
        // given
        TestController testController = new TestController();
        Method method = TestController.class.getMethod("handleWithMultiplePathVariables", String.class, String.class);

        PathPattern pathPattern = new PathPattern("/categories/{category}/products/{productId}");
        RequestMappingInfo requestMappingInfo = new RequestMappingInfo(pathPattern, HttpMethod.GET, testController, method);
        HandlerMethod handlerMethod = new HandlerMethod(requestMappingInfo);

        String requestPath = "/categories/electronics/products/P123";
        when(mockRequest.getPath()).thenReturn(requestPath); // HttpRequest의 경로 설정

        // PathPattern이 실제로 추출할 경로 변수
        Map<String, String> expectedPathVariables = new HashMap<>();
        expectedPathVariables.put("category", "electronics");
        expectedPathVariables.put("productId", "P123");

        Object[] resolvedArgs = {"electronics", "P123"}; // ArgumentResolver가 반환할 인자들
        when(mockResolvers.resolveArguments(eq(handlerMethod.requestMappingInfo().handlerMethod()), eq(mockRequest), eq(expectedPathVariables)))
                .thenReturn(resolvedArgs);

        // when
        Object result = handlerMethodInvoker.invoke(requestMappingInfo, mockRequest);

        // then
        assertThat(result).isEqualTo("multi_path_var_result_electronics_P123");
        // resolveArguments가 추출된 pathVariables와 함께 호출되었는지 검증
        verify(mockResolvers).resolveArguments(eq(handlerMethod.requestMappingInfo().handlerMethod()), eq(mockRequest), eq(expectedPathVariables));
    }

    @Test
    @DisplayName("핸들러 메서드 호출 중 예외 발생 시 예외를 전파해야 한다")
    void invoke_handlerMethodThrowsException_propagatesException() throws Exception {
        // given
        TestController testController = new TestController(); // 이제 handleThrowsException이 직접 정의됨
        Method method = TestController.class.getMethod("handleThrowsException");

        PathPattern pathPattern = new PathPattern("/error-path");
        RequestMappingInfo requestMappingInfo = new RequestMappingInfo(pathPattern, HttpMethod.GET, testController, method);
        HandlerMethod handlerMethod = new HandlerMethod(requestMappingInfo);

        when(mockResolvers.resolveArguments(any(Method.class), any(HttpRequest.class), any(Map.class)))
                .thenReturn(new Object[]{});
        when(mockRequest.getPath()).thenReturn("/error-path");

        // when & then
        assertThrows(InvocationTargetException.class, () ->
                handlerMethodInvoker.invoke(requestMappingInfo, mockRequest)
        );

        verify(mockResolvers).resolveArguments(eq(handlerMethod.requestMappingInfo().handlerMethod()), eq(mockRequest), any(Map.class));
    }

    @Test
    @DisplayName("CompositeArgumentResolver에서 예외 발생 시 예외를 전파해야 한다")
    void invoke_argumentResolutionThrowsException_propagatesException() throws Exception {
        // given
        TestController testController = new TestController();
        Method method = TestController.class.getMethod("handleNoArgs"); // 아무 메서드나 사용

        PathPattern pathPattern = new PathPattern("/arg-error");
        RequestMappingInfo requestMappingInfo = new RequestMappingInfo(pathPattern, HttpMethod.GET, testController, method);
        HandlerMethod handlerMethod = new HandlerMethod(requestMappingInfo);

        // ArgumentResolver가 예외를 던지도록 Mocking
        when(mockResolvers.resolveArguments(any(Method.class), any(HttpRequest.class), any(Map.class)))
                .thenThrow(new IllegalStateException("Failed to resolve argument"));
        when(mockRequest.getPath()).thenReturn("/arg-error");

        // when & then
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                handlerMethodInvoker.invoke(requestMappingInfo, mockRequest)
        );

        assertThat(thrown.getMessage()).isEqualTo("Failed to resolve argument");
        verify(mockResolvers).resolveArguments(eq(handlerMethod.requestMappingInfo().handlerMethod()), eq(mockRequest), any(Map.class));
    }
}