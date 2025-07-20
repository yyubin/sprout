package sprout.server.websocket.handler;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sprout.mvc.http.HttpRequest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultWebSocketHandshakeHandlerTest {

    @Mock
    private HttpRequest<?> mockRequest;

    @InjectMocks
    private DefaultWebSocketHandshakeHandler handshakeHandler;

    private StringWriter stringWriter;
    private BufferedWriter bufferedWriter;

    @BeforeEach
    void setUp() {
        // BufferedWriter가 쓰는 내용을 캡처하기 위해 StringWriter를 사용
        stringWriter = new StringWriter();
        bufferedWriter = new BufferedWriter(stringWriter);
    }

    @Test
    @DisplayName("유효한 헤더가 주어지면 핸드셰이크에 성공하고 101 응답을 반환한다.")
    void performHandshake_shouldSucceedWithValidHeaders() throws IOException {
        // given (준비)
        // RFC 6455에 명시된 예시 키
        String clientKey = "dGhlIHNhbXBsZSBub25jZQ==";
        // 위 키에 대한 올바른 서버 응답 키
        String expectedServerAccept = "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=";

        Map<String, String> validHeaders = new HashMap<>();
        validHeaders.put("Upgrade", "websocket");
        validHeaders.put("Connection", "Upgrade");
        validHeaders.put("Sec-WebSocket-Key", clientKey);
        validHeaders.put("Sec-WebSocket-Version", "13");

        when(mockRequest.getHeaders()).thenReturn(validHeaders);
        when(mockRequest.getPath()).thenReturn("/ws");

        // when (실행)
        boolean success = handshakeHandler.performHandshake(mockRequest, bufferedWriter);

        // then (검증)
        assertTrue(success, "핸드셰이크는 성공해야 합니다.");

        String response = stringWriter.toString();
        assertTrue(response.contains("HTTP/1.1 101 Switching Protocols"), "응답 코드가 101이어야 합니다.");
        assertTrue(response.contains("Upgrade: websocket"), "Upgrade 헤더가 포함되어야 합니다.");
        assertTrue(response.contains("Connection: Upgrade"), "Connection 헤더가 포함되어야 합니다.");
        assertTrue(response.contains("Sec-WebSocket-Accept: " + expectedServerAccept), "계산된 Accept 키가 정확해야 합니다.");
    }

    @Test
    @DisplayName("Upgrade 헤더가 없으면 핸드셰이크에 실패하고 400 응답을 반환한다.")
    void performHandshake_shouldFailWithoutUpgradeHeader() throws IOException {
        // given
        Map<String, String> invalidHeaders = new HashMap<>();
        // invalidHeaders.put("Upgrade", "websocket"); // Upgrade 헤더 누락
        invalidHeaders.put("Connection", "Upgrade");
        invalidHeaders.put("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==");
        invalidHeaders.put("Sec-WebSocket-Version", "13");

        when(mockRequest.getHeaders()).thenReturn(invalidHeaders);

        // when
        boolean success = handshakeHandler.performHandshake(mockRequest, bufferedWriter);

        // then
        assertFalse(success, "핸드셰이크는 실패해야 합니다.");
        String response = stringWriter.toString();
        assertTrue(response.contains("HTTP/1.1 400 Bad Request"), "응답 코드가 400이어야 합니다.");
    }

    @Test
    @DisplayName("Sec-WebSocket-Version이 13이 아니면 핸드셰이크에 실패하고 400 응답을 반환한다.")
    void performHandshake_shouldFailWithInvalidVersion() throws IOException {
        // given
        Map<String, String> invalidHeaders = new HashMap<>();
        invalidHeaders.put("Upgrade", "websocket");
        invalidHeaders.put("Connection", "Upgrade");
        invalidHeaders.put("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==");
        invalidHeaders.put("Sec-WebSocket-Version", "12"); // 잘못된 버전

        when(mockRequest.getHeaders()).thenReturn(invalidHeaders);

        // when
        boolean success = handshakeHandler.performHandshake(mockRequest, bufferedWriter);

        // then
        assertFalse(success, "핸드셰이크는 실패해야 합니다.");
        String response = stringWriter.toString();
        assertTrue(response.contains("HTTP/1.1 400 Bad Request"), "응답 코드가 400이어야 합니다.");
    }

    @Test
    @DisplayName("Sec-WebSocket-Key가 없으면 핸드셰이크에 실패하고 400 응답을 반환한다.")
    void performHandshake_shouldFailWithoutKey() throws IOException {
        // given
        Map<String, String> invalidHeaders = new HashMap<>();
        invalidHeaders.put("Upgrade", "websocket");
        invalidHeaders.put("Connection", "Upgrade");
        // invalidHeaders.put("Sec-WebSocket-Key", "..."); // 키 누락
        invalidHeaders.put("Sec-WebSocket-Version", "13");

        when(mockRequest.getHeaders()).thenReturn(invalidHeaders);

        // when
        boolean success = handshakeHandler.performHandshake(mockRequest, bufferedWriter);

        // then
        assertFalse(success, "핸드셰이크는 실패해야 합니다.");
        String response = stringWriter.toString();
        assertTrue(response.contains("HTTP/1.1 400 Bad Request"), "응답 코드가 400이어야 합니다.");
    }

}