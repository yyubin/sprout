package sprout.server.argument.builtins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.server.websocket.CloseCode;
import sprout.server.websocket.InvocationContext;
import sprout.server.websocket.LifecyclePhase;

import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseCodeArgumentResolverTest {

    @Mock
    private InvocationContext mockContext;
    @Mock
    private Parameter mockParameter;
    @Mock
    private CloseCode mockCloseCode;

    @InjectMocks
    private CloseCodeArgumentResolver resolver;

    // 테스트용 메서드의 파라미터를 가져오기 위한 헬퍼 클래스
    private static class DummyHandler {
        void handleClose(CloseCode code) {}
        void handleOther(String text) {}
    }

    @Test
    @DisplayName("OnClose 단계이고 파라미터가 CloseCode 타입일 때 supports가 true를 반환해야 한다.")
    void supports_shouldReturnTrue_forClosePhaseAndCloseCodeType() throws NoSuchMethodException {
        // given
        Parameter closeCodeParam = DummyHandler.class.getDeclaredMethod("handleClose", CloseCode.class).getParameters()[0];
        when(mockContext.phase()).thenReturn(LifecyclePhase.CLOSE);
        when(mockContext.getCloseCode()).thenReturn(mockCloseCode); // CloseCode가 Context에 존재해야 함

        // when
        boolean result = resolver.supports(closeCodeParam, mockContext);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("단계가 다르거나 파라미터 타입이 다르면 supports가 false를 반환해야 한다.")
    void supports_shouldReturnFalse_forWrongPhaseOrType() throws NoSuchMethodException {
        // given
        Parameter closeCodeParam = DummyHandler.class.getDeclaredMethod("handleClose", CloseCode.class).getParameters()[0];
        Parameter stringParam = DummyHandler.class.getDeclaredMethod("handleOther", String.class).getParameters()[0];

        // when
        when(mockContext.phase()).thenReturn(LifecyclePhase.MESSAGE); // 잘못된 단계
        boolean resultWrongPhase = resolver.supports(closeCodeParam, mockContext);

        when(mockContext.phase()).thenReturn(LifecyclePhase.CLOSE); // 올바른 단계
        boolean resultWrongType = resolver.supports(stringParam, mockContext); // 잘못된 타입

        // then
        assertFalse(resultWrongPhase);
        assertFalse(resultWrongType);
    }

    @Test
    @DisplayName("resolve는 InvocationContext에서 CloseCode 객체를 반환해야 한다.")
    void resolve_shouldReturnCloseCodeFromContext() throws Exception {
        // given
        when(mockContext.getCloseCode()).thenReturn(mockCloseCode);

        // when
        Object result = resolver.resolve(mockParameter, mockContext);

        // then
        assertSame(mockCloseCode, result);
    }
}