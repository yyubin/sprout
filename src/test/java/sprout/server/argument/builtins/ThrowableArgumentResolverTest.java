package sprout.server.argument.builtins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.server.websocket.InvocationContext;
import sprout.server.websocket.LifecyclePhase;

import java.io.IOException;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThrowableArgumentResolverTest {

    @Mock private InvocationContext mockContext;

    @InjectMocks
    private ThrowableArgumentResolver resolver;

    private static class DummyHandler {
        void handleError(Throwable error) {}
    }

    @Test
    @DisplayName("OnError 단계이고 파라미터가 Throwable 타입일 때 supports가 true를 반환해야 한다.")
    void supports_shouldReturnTrue_forErrorPhaseAndThrowableType() throws NoSuchMethodException {
        // given
        Parameter param = DummyHandler.class.getDeclaredMethod("handleError", Throwable.class).getParameters()[0];
        when(mockContext.phase()).thenReturn(LifecyclePhase.ERROR);
        when(mockContext.error()).thenReturn(new RuntimeException()); // 에러 객체가 존재해야 함

        // when
        boolean result = resolver.supports(param, mockContext);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("resolve는 컨텍스트에서 에러 객체를 반환해야 한다.")
    void resolve_shouldReturnErrorFromContext() throws Exception {
        // given
        Throwable testException = new IOException("Test error");
        when(mockContext.error()).thenReturn(testException);

        // when
        Object result = resolver.resolve(null, mockContext);

        // then
        assertSame(testException, result);
    }
}