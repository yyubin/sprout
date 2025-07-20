package sprout.server.websocket.message;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.DispatchResult;
import sprout.server.websocket.InvocationContext;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.WebSocketSession;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractWebSocketMessageDispatcherTest {

    @Mock private List<WebSocketArgumentResolver> mockArgumentResolvers;
    @Mock private WebSocketArgumentResolver mockResolver;
    @Mock private InvocationContext mockContext;
    @Mock private WebSocketSession mockSession;
    @Mock private WebSocketEndpointInfo mockEndpointInfo;
    @Mock private WebSocketFrame mockFrame;
    @Mock private Method mockHandlerMethod;

    private TestDispatcher dispatcher;
    private final Object handlerBean = new Object();

    // --- Test-only concrete implementation of the abstract class ---
    private class TestDispatcher extends AbstractWebSocketMessageDispatcher {
        private DispatchInfo dispatchInfoToReturn;
        public TestDispatcher(List<WebSocketArgumentResolver> argumentResolvers) {
            super(argumentResolvers);
        }
        public void setDispatchInfoToReturn(DispatchInfo info) { this.dispatchInfoToReturn = info; }
        @Override
        protected DispatchInfo prepareDispatchInfo(InvocationContext context) { return this.dispatchInfoToReturn; }
        @Override
        public boolean supports(WebSocketFrame frame, InvocationContext context) { return true; } // Not under test
    }

    @BeforeEach
    void setUp() {
        // 테스트 대상인 TestDispatcher를 생성합니다.
        dispatcher = new TestDispatcher(mockArgumentResolvers);

        // 공통적인 Mock 객체 행동을 설정합니다.
        lenient().when(mockContext.session()).thenReturn(mockSession);
        lenient().when(mockSession.getEndpointInfo()).thenReturn(mockEndpointInfo);
        lenient().when(mockEndpointInfo.getHandlerBean()).thenReturn(handlerBean);
    }

    @Test
    @DisplayName("정상적인 경로와 페이로드로 dispatch가 성공적으로 호출되어야 한다.")
    void dispatch_shouldSucceedOnValidPath() throws Exception {
        // given (준비)
        String destination = "/chat/message";
        Object payload = "Hello World";
        dispatcher.setDispatchInfoToReturn(new AbstractWebSocketMessageDispatcher.DispatchInfo(destination, payload));

        when(mockEndpointInfo.getMessageMappingMethod(destination)).thenReturn(mockHandlerMethod);
        when(mockHandlerMethod.getParameters()).thenReturn(new java.lang.reflect.Parameter[0]); // 파라미터 없는 메서드로 가정

        // when (실행)
        DispatchResult result = dispatcher.dispatch(mockFrame, mockContext);

        // then (검증)
        assertTrue(result.isHandled());
        verify(mockEndpointInfo).getMessageMappingMethod(destination);
        verify(mockHandlerMethod).invoke(handlerBean); // 인자 없이 호출되었는지 확인
    }

    @Test
    @DisplayName("prepareDispatchInfo가 null을 반환하면 dispatch가 중단되어야 한다.")
    void dispatch_shouldFailWhenPrepareDispatchInfoReturnsNull() throws Exception {
        // given
        dispatcher.setDispatchInfoToReturn(null);

        // when
        DispatchResult result = dispatcher.dispatch(mockFrame, mockContext);

        // then
        assertFalse(result.isHandled());
        verify(mockEndpointInfo, never()).getMessageMappingMethod(any()); // 메서드 찾기 로직이 호출되지 않아야 함
    }

    @Test
    @DisplayName("목적지에 맞는 핸들러 메서드가 없으면 dispatch가 중단되어야 한다.")
    void dispatch_shouldFailWhenNoMethodMatchesDestination() throws Exception {
        // given
        String destination = "/unknown/path";
        dispatcher.setDispatchInfoToReturn(new AbstractWebSocketMessageDispatcher.DispatchInfo(destination, "payload"));
        when(mockEndpointInfo.getMessageMappingMethod(destination)).thenReturn(null); // 메서드 못 찾음

        // when
        DispatchResult result = dispatcher.dispatch(mockFrame, mockContext);

        // then
        assertFalse(result.isHandled());
        verify(mockHandlerMethod, never()).invoke(any(), any()); // 메서드 호출 로직이 실행되지 않아야 함
    }

    @Test
    @DisplayName("ArgumentResolver가 파라미터를 해석하지 못하면 예외가 발생해야 한다.")
    void dispatch_shouldThrowExceptionWhenArgumentCannotBeResolved() throws Exception {
        // given
        String destination = "/test";
        dispatcher.setDispatchInfoToReturn(new AbstractWebSocketMessageDispatcher.DispatchInfo(destination, "payload"));

        // String type의 파라미터를 가진 가짜 메서드 설정
        Method methodWithParam = TestHandler.class.getMethod("handle", String.class);
        when(mockEndpointInfo.getMessageMappingMethod(destination)).thenReturn(methodWithParam);
        when(mockArgumentResolvers.iterator()).thenReturn(List.of(mockResolver).iterator());
        when(mockResolver.supports(any(), any())).thenReturn(false); // 어떤 파라미터도 지원하지 않음

        // when & then
        assertThrows(IllegalArgumentException.class, () -> {
            dispatcher.dispatch(mockFrame, mockContext);
        }, "해결할 수 없는 파라미터가 있으면 IllegalArgumentException이 발생해야 합니다.");
    }

    // 테스트용 가짜 핸들러 클래스
    private static class TestHandler {
        public void handle(String message) {}
    }

}