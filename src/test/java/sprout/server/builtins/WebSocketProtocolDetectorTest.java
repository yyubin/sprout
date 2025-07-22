package sprout.server.builtins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebSocketProtocolDetectorTest {

    private final WebSocketProtocolDetector detector = new WebSocketProtocolDetector();

    @DisplayName("웹소켓 업그레이드 헤더가 존재하면 'WEBSOCKET'을 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {
            "GET /chat HTTP/1.1\r\nHost: example.com\r\nUpgrade: websocket\r\nConnection: Upgrade\r\n\r\n",
            "GET /chat HTTP/1.1\nHost: example.com\nUpgrade: websocket\nConnection: Upgrade\n\n"
    })
    void should_return_websocket_when_header_is_present(String webSocketRequest) throws Exception {
        // given
        ByteBuffer buffer = ByteBuffer.wrap(webSocketRequest.getBytes(StandardCharsets.UTF_8));
        int initialPosition = buffer.position();

        // when
        String result = detector.detect(buffer);

        // then
        assertEquals("WEBSOCKET", result);
        assertEquals(initialPosition, buffer.position(), "Buffer position should be reset");
    }

    @Test
    @DisplayName("웹소켓 업그레이드 헤더가 없으면 'UNKNOWN'을 반환한다")
    void should_return_unknown_when_header_is_absent() throws Exception {
        // given
        String httpRequest = "GET /index.html HTTP/1.1\r\nHost: example.com\r\n\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(httpRequest.getBytes(StandardCharsets.UTF_8));
        int initialPosition = buffer.position();

        // when
        String result = detector.detect(buffer);

        // then
        assertEquals("UNKNOWN", result);
        assertEquals(initialPosition, buffer.position(), "Buffer position should be reset");
    }

    @Test
    @DisplayName("빈 버퍼가 주어지면 'UNKNOWN'을 반환한다")
    void should_return_unknown_for_empty_buffer() throws Exception {
        // given
        ByteBuffer buffer = ByteBuffer.allocate(0);
        int initialPosition = buffer.position();

        // when
        String result = detector.detect(buffer);

        // then
        assertEquals("UNKNOWN", result);
        assertEquals(initialPosition, buffer.position(), "Buffer position should be reset");
    }
}