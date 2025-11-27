package sprout.server.builtins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpProtocolDetectorTest {

    private final HttpProtocolDetector detector = new HttpProtocolDetector();

    @DisplayName("버퍼가 HTTP 메서드로 시작하면 'HTTP/1.1'을 반환한다")
    @ParameterizedTest
    @ValueSource(strings = {"GET ", "POST ", "PUT ", "DELETE ", "HEAD ", "OPTIONS ", "PATCH ", "TRACE "})
    void should_return_http_when_buffer_starts_with_http_method(String httpMethod) throws Exception {
        // given
        String httpRequest = httpMethod + "/index.html HTTP/1.1\r\nHost: example.com\r\n\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(httpRequest.getBytes(StandardCharsets.UTF_8));

        // 버퍼의 초기 위치를 기록
        int initialPosition = buffer.position();

        // when
        String detectedProtocol = detector.detect(buffer);

        // then
        assertEquals("HTTP/1.1", detectedProtocol);
        // detect 메서드 실행 후 버퍼의 위치가 원래대로 돌아왔는지 확인
        assertEquals(initialPosition, buffer.position());
    }

    @Test
    @DisplayName("버퍼가 HTTP 메서드로 시작하지 않으면 'UNKNOWN'을 반환한다")
    void should_return_unknown_when_buffer_does_not_start_with_http_method() throws Exception {
        // given
        String nonHttpRequest = "This is not an HTTP request.";
        ByteBuffer buffer = ByteBuffer.wrap(nonHttpRequest.getBytes(StandardCharsets.UTF_8));

        int initialPosition = buffer.position();

        // when
        String detectedProtocol = detector.detect(buffer);

        // then
        assertEquals("UNKNOWN", detectedProtocol);
        assertEquals(initialPosition, buffer.position());
    }

    @Test
    @DisplayName("빈 버퍼가 주어지면 'UNKNOWN'을 반환한다")
    void should_return_unknown_for_empty_buffer() throws Exception {
        // given
        ByteBuffer buffer = ByteBuffer.allocate(0);
        int initialPosition = buffer.position();

        // when
        String detectedProtocol = detector.detect(buffer);

        // then
        assertEquals("UNKNOWN", detectedProtocol);
        assertEquals(initialPosition, buffer.position());
    }

    @Test
    @DisplayName("WebSocket Upgrade 요청은 UNKNOWN을 반환한다 (WebSocketProtocolDetector가 처리하도록)")
    void should_return_unknown_for_websocket_upgrade_request() throws Exception {
        // given
        String websocketRequest = "GET /ws/benchmark HTTP/1.1\r\n" +
                                 "Host: localhost:8080\r\n" +
                                 "Upgrade: websocket\r\n" +
                                 "Connection: Upgrade\r\n" +
                                 "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n" +
                                 "Sec-WebSocket-Version: 13\r\n\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(websocketRequest.getBytes(StandardCharsets.UTF_8));
        int initialPosition = buffer.position();

        // when
        String detectedProtocol = detector.detect(buffer);

        // then
        assertEquals("UNKNOWN", detectedProtocol, "WebSocket 요청은 UNKNOWN을 반환해야 합니다");
        assertEquals(initialPosition, buffer.position(), "버퍼 위치가 복원되어야 합니다");
    }

    @Test
    @DisplayName("대소문자 구분 없이 Upgrade: WebSocket 헤더도 UNKNOWN을 반환한다")
    void should_return_unknown_for_websocket_with_capital_letters() throws Exception {
        // given
        String websocketRequest = "GET /ws HTTP/1.1\r\n" +
                                 "Host: example.com\r\n" +
                                 "Upgrade: WebSocket\r\n" +
                                 "Connection: Upgrade\r\n\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(websocketRequest.getBytes(StandardCharsets.UTF_8));

        // when
        String detectedProtocol = detector.detect(buffer);

        // then
        assertEquals("UNKNOWN", detectedProtocol);
    }

    @Test
    @DisplayName("일반 HTTP 요청 (Upgrade 헤더 없음)은 HTTP/1.1을 반환한다")
    void should_return_http_for_normal_http_request() throws Exception {
        // given
        String normalHttpRequest = "GET /api/users HTTP/1.1\r\n" +
                                   "Host: example.com\r\n" +
                                   "Accept: application/json\r\n\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(normalHttpRequest.getBytes(StandardCharsets.UTF_8));

        // when
        String detectedProtocol = detector.detect(buffer);

        // then
        assertEquals("HTTP/1.1", detectedProtocol, "일반 HTTP 요청은 HTTP/1.1을 반환해야 합니다");
    }

    @Test
    @DisplayName("Connection 헤더에 다중 값이 있는 WebSocket 요청은 UNKNOWN을 반환한다")
    void should_return_unknown_for_websocket_with_multiple_connection_values() throws Exception {
        // given
        String websocketRequest = "GET /ws HTTP/1.1\r\n" +
                                 "Host: localhost:8080\r\n" +
                                 "Connection: keep-alive, Upgrade\r\n" +
                                 "Upgrade: websocket\r\n\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(websocketRequest.getBytes(StandardCharsets.UTF_8));

        // when
        String detectedProtocol = detector.detect(buffer);

        // then
        assertEquals("UNKNOWN", detectedProtocol);
    }
}