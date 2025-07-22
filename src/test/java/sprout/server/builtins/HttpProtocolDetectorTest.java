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
}