package sprout.server.builtins;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebSocketProtocolDetectorTest {
    private WebSocketProtocolDetector detector;

    @BeforeEach
    void setUp() {
        detector = new WebSocketProtocolDetector();
    }

    @Test
    @DisplayName("WebSocket 업그레이드 헤더가 있으면 WEBSOCKET을 반환해야 한다.")
    void detect_shouldReturnWebSocket_whenUpgradeHeaderExists() throws Exception {
        // given
        String request = """
                GET /chat HTTP/1.1\r
                Host: example.com\r
                Upgrade: websocket\r
                Connection: Upgrade\r
                Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r
                Sec-WebSocket-Version: 13\r
                \r
                """;
        InputStream inputStream = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));

        // when
        String protocol = detector.detect(inputStream);

        // then
        assertEquals("WEBSOCKET", protocol);
    }

    @Test
    @DisplayName("LF 줄바꿈을 사용하는 WebSocket 헤더도 감지해야 한다.")
    void detect_shouldReturnWebSocket_withLfLineEndings() throws Exception {
        // given
        String request = "GET /chat HTTP/1.1\n" +
                "Host: example.com\n" +
                "Upgrade: websocket\n" +
                "Connection: Upgrade\n";
        InputStream inputStream = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));

        // when
        String protocol = detector.detect(inputStream);

        // then
        assertEquals("WEBSOCKET", protocol);
    }

    @Test
    @DisplayName("일반 HTTP 요청은 UNKNOWN을 반환해야 한다.")
    void detect_shouldReturnUnknown_forStandardHttpRequest() throws Exception {
        // given
        String request = """
                GET /index.html HTTP/1.1\r
                Host: example.com\r
                Connection: keep-alive\r
                \r
                """;
        InputStream inputStream = new ByteArrayInputStream(request.getBytes(StandardCharsets.UTF_8));

        // when
        String protocol = detector.detect(inputStream);

        // then
        assertEquals("UNKNOWN", protocol);
    }

    @Test
    @DisplayName("빈 스트림은 UNKNOWN을 반환해야 한다.")
    void detect_shouldReturnUnknown_forEmptyStream() throws Exception {
        // given
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        // when
        String protocol = detector.detect(inputStream);

        // then
        assertEquals("UNKNOWN", protocol);
    }

    @Test
    @DisplayName("프로토콜 감지 후 스트림은 초기 상태로 리셋되어야 한다.")
    void detect_shouldResetStream_afterDetection() throws Exception {
        // given
        String originalRequest = "GET /chat HTTP/1.1\r\nUpgrade: websocket\r\n\r\n";
        byte[] originalBytes = originalRequest.getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(originalBytes);

        // when
        detector.detect(inputStream); // 프로토콜 감지 실행

        // then
        // 스트림을 다시 읽어서 내용이 그대로인지 확인
        byte[] bytesAfterDetection = inputStream.readAllBytes();
        assertEquals(originalRequest, new String(bytesAfterDetection, StandardCharsets.UTF_8),
                "detect() 호출 후에도 스트림의 내용은 원본과 동일해야 합니다.");
    }
}