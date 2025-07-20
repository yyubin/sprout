package sprout.server.argument.builtins;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.server.argument.annotation.Payload;
import sprout.server.websocket.InvocationContext;
import sprout.server.websocket.LifecyclePhase;
import sprout.server.websocket.message.MessagePayload;

import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonPayloadArgumentResolverTest {

    @Mock private InvocationContext mockContext;
    @Mock private MessagePayload mockMessagePayload;
    @Spy private ObjectMapper objectMapper; // 실제 JSON 변환을 테스트하기 위해 @Spy 사용

    @InjectMocks
    private JsonPayloadArgumentResolver resolver;

    // 테스트용 DTO
    private static class UserDto {
        public String name;
        public int age;
    }

    private static class DummyHandler {
        void handleJson(@Payload UserDto user) {}
        void handleString(@Payload String text) {}
    }

    @Test
    @DisplayName("@Payload가 있고 String 타입이 아닐 때 supports가 true를 반환해야 한다.")
    void supports_shouldReturnTrue_forPayloadAndNonStringType() throws NoSuchMethodException {
        // given
        Parameter param = DummyHandler.class.getDeclaredMethod("handleJson", UserDto.class).getParameters()[0];
        when(mockContext.phase()).thenReturn(LifecyclePhase.MESSAGE);
        when(mockContext.getMessagePayload()).thenReturn(mockMessagePayload);

        // when
        boolean result = resolver.supports(param, mockContext);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("파라미터 타입이 String이면 false를 반환해야 한다.")
    void supports_shouldReturnFalse_forStringType() throws NoSuchMethodException {
        // given
        Parameter param = DummyHandler.class.getDeclaredMethod("handleString", String.class).getParameters()[0];
        when(mockContext.phase()).thenReturn(LifecyclePhase.MESSAGE);
        when(mockContext.getMessagePayload()).thenReturn(mockMessagePayload);

        // when
        boolean result = resolver.supports(param, mockContext);

        // then
        assertFalse(result); // String은 다른 Resolver가 처리
    }

    @Test
    @DisplayName("resolve는 메시지 페이로드를 JSON 파싱하여 객체로 반환해야 한다.")
    void resolve_shouldReturnJsonObjectFromPayload() throws Exception {
        // given
        Parameter param = DummyHandler.class.getDeclaredMethod("handleJson", UserDto.class).getParameters()[0];
        String json = "{\"name\":\"test-user\",\"age\":30}";
        when(mockContext.getMessagePayload()).thenReturn(mockMessagePayload);
        when(mockMessagePayload.asText()).thenReturn(json);

        // when
        Object result = resolver.resolve(param, mockContext);

        // then
        assertNotNull(result);
        assertInstanceOf(UserDto.class, result);
        UserDto user = (UserDto) result;
        assertEquals("test-user", user.name);
        assertEquals(30, user.age);
    }
}