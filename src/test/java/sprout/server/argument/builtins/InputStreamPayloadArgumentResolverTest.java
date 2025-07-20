package sprout.server.argument.builtins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.server.argument.annotation.Payload;
import sprout.server.websocket.InvocationContext;
import sprout.server.websocket.LifecyclePhase;
import sprout.server.websocket.WebSocketFrame;

import java.io.InputStream;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InputStreamPayloadArgumentResolverTest {

    @Mock private InvocationContext mockContext;
    @Mock private WebSocketFrame mockFrame;
    @Mock private InputStream mockInputStream;

    @InjectMocks
    private InputStreamPayloadArgumentResolver resolver;

    private static class DummyHandler {
        void handleStream(@Payload InputStream stream) {}
        void handleNonPayload(InputStream stream) {}
    }

    @Test
    @DisplayName("모든 조건이 맞을 때 supports가 true를 반환해야 한다.")
    void supports_shouldReturnTrue_whenAllConditionsMet() throws NoSuchMethodException {
        // given
        Parameter param = DummyHandler.class.getDeclaredMethod("handleStream", InputStream.class).getParameters()[0];
        when(mockContext.phase()).thenReturn(LifecyclePhase.MESSAGE);
        when(mockContext.getFrame()).thenReturn(mockFrame);
        when(mockContext.getMessagePayload()).thenReturn(null); // 스트리밍 의도
        when(mockContext.isFin()).thenReturn(true);

        // when
        boolean result = resolver.supports(param, mockContext);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("@Payload 어노테이션이 없으면 false를 반환해야 한다.")
    void supports_shouldReturnFalse_whenNoPayloadAnnotation() throws NoSuchMethodException {
        // given
        Parameter param = DummyHandler.class.getDeclaredMethod("handleNonPayload", InputStream.class).getParameters()[0];
        lenient().when(mockContext.phase()).thenReturn(LifecyclePhase.MESSAGE); // 다른 조건은 모두 true여도

        // when
        boolean result = resolver.supports(param, mockContext);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("resolve는 프레임에서 페이로드 스트림을 반환해야 한다.")
    void resolve_shouldReturnPayloadStreamFromFrame() throws Exception {
        // given
        when(mockContext.getFrame()).thenReturn(mockFrame);
        when(mockFrame.getPayloadStream()).thenReturn(mockInputStream);

        // when
        Object result = resolver.resolve(null, mockContext); // parameter는 사용되지 않음

        // then
        assertSame(mockInputStream, result);
    }
}