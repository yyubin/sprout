package sprout.server.argument.builtins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.server.websocket.InvocationContext;
import sprout.server.websocket.WebSocketSession;

import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionArgumentResolverTest {

    @Mock private InvocationContext mockContext;
    @Mock private WebSocketSession mockSession;

    @InjectMocks
    private SessionArgumentResolver resolver;

    private static class DummyHandler {
        void handle(WebSocketSession session) {}
    }

    @Test
    @DisplayName("파라미터 타입이 WebSocketSession일 때 supports가 true를 반환해야 한다.")
    void supports_shouldReturnTrue_forWebSocketSessionType() throws NoSuchMethodException {
        // given
        Parameter param = DummyHandler.class.getDeclaredMethod("handle", WebSocketSession.class).getParameters()[0];
        when(mockContext.session()).thenReturn(mockSession);

        // when
        boolean result = resolver.supports(param, mockContext);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("resolve는 컨텍스트에서 현재 세션 객체를 반환해야 한다.")
    void resolve_shouldReturnSessionFromContext() {
        // given
        when(mockContext.session()).thenReturn(mockSession);

        // when
        Object result = resolver.resolve(null, mockContext);

        // then
        assertSame(mockSession, result);
    }
}