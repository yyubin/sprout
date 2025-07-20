package sprout.server.websocket.message.builtins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.server.websocket.InvocationContext;
import sprout.server.websocket.message.MessagePayload;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.message.AbstractWebSocketMessageDispatcher.DispatchInfo;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JsonWebSocketMessageDispatcherTest {
    private JsonWebSocketMessageDispatcher dispatcher;

    @Mock
    private InvocationContext mockContext;

    @Mock
    private WebSocketFrame mockFrame;

    @Mock
    private MessagePayload mockMessagePayload;

    @BeforeEach
    void setUp() {
        // 실제 ObjectMapper를 사용하여 JSON 파싱 기능을 직접 테스트합니다.
        ObjectMapper objectMapper = new ObjectMapper();
        // argumentResolvers는 부모 클래스에 필요하지만, 이 테스트에서는 사용되지 않으므로 빈 리스트를 전달합니다.
        dispatcher = new JsonWebSocketMessageDispatcher(objectMapper, Collections.emptyList());

        // 공통 Mock 설정
        lenient().when(mockContext.getMessagePayload()).thenReturn(mockMessagePayload);
    }

    @Nested
    @DisplayName("supports() 메서드 테스트")
    class SupportsTest {

        @Test
        @DisplayName("텍스트 프레임(opcode 0x1)이고 페이로드가 텍스트일 때 true를 반환해야 한다.")
        void shouldReturnTrue_forTextFrameAndTextPayload() {
            // given
            when(mockFrame.getOpcode()).thenReturn(0x1); // TEXT_FRAME
            when(mockMessagePayload.isText()).thenReturn(true);

            // when
            boolean result = dispatcher.supports(mockFrame, mockContext);

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("바이너리 프레임(opcode 0x2)일 때 false를 반환해야 한다.")
        void shouldReturnFalse_forBinaryFrame() {
            // given
            when(mockFrame.getOpcode()).thenReturn(0x2); // BINARY_FRAME
            // isText() 호출 여부와 상관없이 opcode에서 걸러져야 함

            // when
            boolean result = dispatcher.supports(mockFrame, mockContext);

            // then
            assertFalse(result);
        }

        @Test
        @DisplayName("페이로드가 텍스트가 아닐 때 false를 반환해야 한다.")
        void shouldReturnFalse_whenPayloadIsNotText() {
            // given
            when(mockFrame.getOpcode()).thenReturn(0x1); // TEXT_FRAME
            when(mockMessagePayload.isText()).thenReturn(false);

            // when
            boolean result = dispatcher.supports(mockFrame, mockContext);

            // then
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("prepareDispatchInfo() 메서드 테스트")
    class PrepareDispatchInfoTest {

        @Test
        @DisplayName("유효한 JSON 문자열을 정확히 파싱하여 DispatchInfo 객체를 생성해야 한다.")
        void shouldParseJsonAndReturnCorrectDispatchInfo() throws Exception {
            // given
            String jsonContent = "{\"destination\":\"/topic/greetings\",\"payload\":\"Hello Sprout\"}";
            when(mockMessagePayload.asText()).thenReturn(jsonContent);

            // when
            DispatchInfo result = dispatcher.prepareDispatchInfo(mockContext);

            // then
            assertNotNull(result);
            assertEquals("/topic/greetings", result.destination());
            assertEquals("Hello Sprout", result.payload());
        }

        @Test
        @DisplayName("유효하지 않은 JSON 문자열에 대해 예외를 발생시켜야 한다.")
        void shouldThrowException_forInvalidJson() {
            // given
            String invalidJson = "{\"destination\":\"/topic/greetings\","; // 닫히지 않은 JSON
            when(mockMessagePayload.asText()).thenReturn(invalidJson);

            // when & then
            assertThrows(JsonProcessingException.class, () -> {
                dispatcher.prepareDispatchInfo(mockContext);
            }, "잘못된 형식의 JSON은 JsonProcessingException을 발생시켜야 합니다.");
        }
    }
}