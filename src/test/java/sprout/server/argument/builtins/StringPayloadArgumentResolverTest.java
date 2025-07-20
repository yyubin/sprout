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
import sprout.server.websocket.message.MessagePayload;

import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StringPayloadArgumentResolverTest {

    @Mock private InvocationContext mockContext;
    @Mock private MessagePayload mockMessagePayload;

    @InjectMocks
    private StringPayloadArgumentResolver resolver;

    private static class DummyHandler {
        void handle(@Payload String message, String notPayload) {}
    }

    @Test
    @DisplayName("모든 조건이 맞을 때 supports가 true를 반환해야 한다.")
    void supports_shouldReturnTrue_whenAllConditionsMet() throws NoSuchMethodException {
        // given
        Parameter param = DummyHandler.class.getDeclaredMethod("handle", String.class, String.class).getParameters()[0];
        when(mockContext.phase()).thenReturn(LifecyclePhase.MESSAGE);
        when(mockContext.getMessagePayload()).thenReturn(mockMessagePayload);
        when(mockMessagePayload.isText()).thenReturn(true);
        when(mockMessagePayload.asText()).thenReturn("some text");

        // when
        boolean result = resolver.supports(param, mockContext);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("resolve는 페이로드의 텍스트 내용을 반환해야 한다.")
    void resolve_shouldReturnTextFromPayload() throws Exception {
        // given
        String messageText = "Hello, Sprout!";
        when(mockContext.getMessagePayload()).thenReturn(mockMessagePayload);
        when(mockMessagePayload.asText()).thenReturn(messageText);

        // when
        Object result = resolver.resolve(null, mockContext);

        // then
        assertEquals(messageText, result);
    }
}