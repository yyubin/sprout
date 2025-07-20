package sprout.server.websocket.message.builtins;

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
class RawBinaryWebSocketMessageDispatcherTest {
    private RawBinaryWebSocketMessageDispatcher dispatcher;

    @Mock
    private InvocationContext mockContext;

    @Mock
    private WebSocketFrame mockFrame;

    @Mock
    private MessagePayload mockMessagePayload;

    @BeforeEach
    void setUp() {
        // argumentResolvers는 이 테스트에서 사용되지 않으므로 빈 리스트를 전달합니다.
        dispatcher = new RawBinaryWebSocketMessageDispatcher(Collections.emptyList());
        // 공통 Mock 설정
        lenient().when(mockContext.getMessagePayload()).thenReturn(mockMessagePayload);
    }

    @Nested
    @DisplayName("supports() 메서드 테스트")
    class SupportsTest {

        @Test
        @DisplayName("바이너리 프레임(opcode 0x2)이고 페이로드가 바이너리일 때 true를 반환해야 한다.")
        void shouldReturnTrue_forBinaryFrameAndBinaryPayload() {
            // given
            when(mockFrame.getOpcode()).thenReturn(0x2); // BINARY_FRAME
            when(mockMessagePayload.isBinary()).thenReturn(true);

            // when
            boolean result = dispatcher.supports(mockFrame, mockContext);

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("텍스트 프레임(opcode 0x1)일 때 false를 반환해야 한다.")
        void shouldReturnFalse_forTextFrame() {
            // given
            when(mockFrame.getOpcode()).thenReturn(0x1); // TEXT_FRAME

            // when
            boolean result = dispatcher.supports(mockFrame, mockContext);

            // then
            assertFalse(result);
        }

        @Test
        @DisplayName("페이로드가 바이너리가 아닐 때 false를 반환해야 한다.")
        void shouldReturnFalse_whenPayloadIsNotBinary() {
            // given
            when(mockFrame.getOpcode()).thenReturn(0x2); // BINARY_FRAME
            when(mockMessagePayload.isBinary()).thenReturn(false);

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
        @DisplayName("고정된 목적지 '/binary'와 원본 바이너리 페이로드를 담은 DispatchInfo를 반환해야 한다.")
        void shouldReturnFixedDestinationAndRawPayload() {
            // given
            byte[] binaryData = new byte[]{10, 20, 30, 40, 50};
            when(mockMessagePayload.asBinary()).thenReturn(binaryData);

            // when
            DispatchInfo result = dispatcher.prepareDispatchInfo(mockContext);

            // then
            assertNotNull(result);
            assertEquals("/binary", result.destination(), "목적지는 항상 '/binary'여야 합니다.");
            assertArrayEquals(binaryData, (byte[]) result.payload(), "페이로드는 원본 byte 배열이어야 합니다.");
        }
    }
}