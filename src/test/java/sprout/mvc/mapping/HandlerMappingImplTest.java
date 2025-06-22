package sprout.mvc.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sprout.mvc.http.HttpMethod;
import sprout.mvc.invoke.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HandlerMappingImplTest {

    private HandlerMappingImpl handlerMapping;

    @Mock
    private RequestMappingRegistry mockRegistry;

    // 테스트용 더미 컨트롤러
    static class TestController {
        public void handleGet() {}
        public void handlePost() {}
    }

    private TestController testController; // 실제 컨트롤러 인스턴스 (HandlerMethod 생성에 필요)

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testController = new TestController();
        handlerMapping = new HandlerMappingImpl(mockRegistry);
    }

    @Test
    @DisplayName("Registry가 핸들러를 찾으면 HandlerMethod를 반환해야 한다.")
    void findHandler_registryFindsHandler_returnsHandlerMethod() throws NoSuchMethodException {
        // given
        String path = "/test";
        HttpMethod httpMethod = HttpMethod.GET;

        // Mock registry가 반환할 RequestMappingInfo 생성
        PathPattern pathPattern = new PathPattern(path);
        Method handlerJavaMethod = testController.getClass().getMethod("handleGet");
        RequestMappingInfo mockRequestMappingInfo = new RequestMappingInfo(
                pathPattern, httpMethod, testController, handlerJavaMethod);

        // --- 여기를 수정했습니다: cast to RequestMappingInfo<?> ---
        when(mockRegistry.getHandlerMethod(eq(path), eq(httpMethod)))
                .thenReturn((RequestMappingInfo) mockRequestMappingInfo); // 명시적 캐스팅

        // when
        HandlerMethod resultHandlerMethod = handlerMapping.findHandler(path, httpMethod);

        // then
        assertNotNull(resultHandlerMethod);
        assertThat(resultHandlerMethod.requestMappingInfo()).isEqualTo(mockRequestMappingInfo);

        // mockRegistry의 메서드가 예상대로 호출되었는지 검증
        verify(mockRegistry).getHandlerMethod(eq(path), eq(httpMethod));
    }

    @Test
    @DisplayName("Registry가 핸들러를 찾지 못하면 null을 반환해야 한다.")
    void findHandler_registryFindsNoHandler_returnsNull() {
        // given
        String path = "/nonexistent";
        HttpMethod httpMethod = HttpMethod.POST;

        // mockRegistry의 getHandlerMethod 호출 시 null 반환하도록 설정
        when(mockRegistry.getHandlerMethod(any(String.class), any(HttpMethod.class)))
                .thenReturn(null); // null은 모든 참조 타입에 대해 유효하므로 캐스팅 불필요

        // when
        HandlerMethod resultHandlerMethod = handlerMapping.findHandler(path, httpMethod);

        // then
        assertNull(resultHandlerMethod);
        verify(mockRegistry).getHandlerMethod(eq(path), eq(httpMethod));
    }

    @Test
    @DisplayName("다른 HTTP 메서드 요청 시 null을 반환해야 한다.")
    void findHandler_differentHttpMethod_returnsNull() throws NoSuchMethodException {
        // given
        String path = "/user";
        HttpMethod registeredMethod = HttpMethod.GET;
        HttpMethod requestedMethod = HttpMethod.POST;

        PathPattern pathPattern = new PathPattern(path);
        Method handlerJavaMethod = testController.getClass().getMethod("handleGet");
        RequestMappingInfo mockRequestMappingInfo = new RequestMappingInfo(
                pathPattern, registeredMethod, testController, handlerJavaMethod);

        // Registry는 GET 메서드에 대한 정보만 가지고 있다고 가정
        when(mockRegistry.getHandlerMethod(eq(path), eq(registeredMethod)))
                .thenReturn((RequestMappingInfo) mockRequestMappingInfo); // 명시적 캐스팅
        // POST 메서드에 대한 요청은 null 반환
        when(mockRegistry.getHandlerMethod(eq(path), eq(requestedMethod)))
                .thenReturn(null);


        // when & then
        // GET 요청은 찾아져야 함
        HandlerMethod getResult = handlerMapping.findHandler(path, registeredMethod);
        assertNotNull(getResult);
        assertThat(getResult.requestMappingInfo()).isEqualTo(mockRequestMappingInfo);

        // POST 요청은 찾아지지 않아야 함
        HandlerMethod postResult = handlerMapping.findHandler(path, requestedMethod);
        assertNull(postResult);

        verify(mockRegistry).getHandlerMethod(eq(path), eq(registeredMethod));
        verify(mockRegistry).getHandlerMethod(eq(path), eq(requestedMethod));
    }
}