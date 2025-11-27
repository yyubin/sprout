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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultWebSocketHandshakeHandlerTest {

    @Mock
    private HttpRequest<?> mockRequest;

    @Mock
    private SocketChannel mockChannel;

    @InjectMocks
    private DefaultWebSocketHandshakeHandler handshakeHandler;

    private ByteBuffer responseBuffer;

    @BeforeEach
    void setUp() throws IOException {
        // SocketChannel.write() 호출 시 ByteBuffer 캡처
        responseBuffer = ByteBuffer.allocate(4096);

        when(mockChannel.write(any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer buf = invocation.getArgument(0);
            int remaining = buf.remaining();

            // responseBuffer에 복사
            byte[] bytes = new byte[remaining];
            buf.get(bytes);
            responseBuffer.put(bytes);

            return remaining;
        });
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
        boolean success = handshakeHandler.performHandshake(mockRequest, mockChannel);

        // then (검증)
        assertTrue(success, "핸드셰이크는 성공해야 합니다.");

        responseBuffer.flip();
        String response = StandardCharsets.UTF_8.decode(responseBuffer).toString();

        assertTrue(response.contains("HTTP/1.1 101 Switching Protocols"), "응답 코드가 101이어야 합니다.");
        assertTrue(response.contains("Upgrade: websocket"), "Upgrade 헤더가 포함되어야 합니다.");
        assertTrue(response.contains("Connection: Upgrade"), "Connection 헤더가 포함되어야 합니다.");
        assertTrue(response.contains("Sec-WebSocket-Accept: " + expectedServerAccept), "계산된 Accept 키가 정확해야 합니다.");

        verify(mockChannel, atLeastOnce()).write(any(ByteBuffer.class));
    }

    @Test
    @DisplayName("Connection 헤더에 다중 값이 있어도 Upgrade가 포함되면 성공한다.")
    void performHandshake_shouldSucceedWithMultipleConnectionValues() throws IOException {
        // given
        String clientKey = "dGhlIHNhbXBsZSBub25jZQ==";

        Map<String, String> validHeaders = new HashMap<>();
        validHeaders.put("Upgrade", "websocket");
        validHeaders.put("Connection", "keep-alive, Upgrade"); // 다중 값
        validHeaders.put("Sec-WebSocket-Key", clientKey);
        validHeaders.put("Sec-WebSocket-Version", "13");

        when(mockRequest.getHeaders()).thenReturn(validHeaders);
        when(mockRequest.getPath()).thenReturn("/ws");

        // when
        boolean success = handshakeHandler.performHandshake(mockRequest, mockChannel);

        // then
        assertTrue(success, "Connection 헤더에 Upgrade가 포함되어 있으면 성공해야 합니다.");

        responseBuffer.flip();
        String response = StandardCharsets.UTF_8.decode(responseBuffer).toString();
        assertTrue(response.contains("HTTP/1.1 101 Switching Protocols"));
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
        boolean success = handshakeHandler.performHandshake(mockRequest, mockChannel);

        // then
        assertFalse(success, "핸드셰이크는 실패해야 합니다.");

        responseBuffer.flip();
        String response = StandardCharsets.UTF_8.decode(responseBuffer).toString();
        assertTrue(response.contains("HTTP/1.1 400 Bad Request"), "응답 코드가 400이어야 합니다.");
    }

    @Test
    @DisplayName("Connection 헤더에 Upgrade가 포함되지 않으면 실패한다.")
    void performHandshake_shouldFailWithoutUpgradeInConnection() throws IOException {
        // given
        Map<String, String> invalidHeaders = new HashMap<>();
        invalidHeaders.put("Upgrade", "websocket");
        invalidHeaders.put("Connection", "keep-alive"); // Upgrade 미포함
        invalidHeaders.put("Sec-WebSocket-Key", "dGhlIHNhbXBsZSBub25jZQ==");
        invalidHeaders.put("Sec-WebSocket-Version", "13");

        when(mockRequest.getHeaders()).thenReturn(invalidHeaders);

        // when
        boolean success = handshakeHandler.performHandshake(mockRequest, mockChannel);

        // then
        assertFalse(success, "Connection 헤더에 Upgrade가 없으면 실패해야 합니다.");

        responseBuffer.flip();
        String response = StandardCharsets.UTF_8.decode(responseBuffer).toString();
        assertTrue(response.contains("HTTP/1.1 400 Bad Request"));
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
        boolean success = handshakeHandler.performHandshake(mockRequest, mockChannel);

        // then
        assertFalse(success, "핸드셰이크는 실패해야 합니다.");

        responseBuffer.flip();
        String response = StandardCharsets.UTF_8.decode(responseBuffer).toString();
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
        boolean success = handshakeHandler.performHandshake(mockRequest, mockChannel);

        // then
        assertFalse(success, "핸드셰이크는 실패해야 합니다.");

        responseBuffer.flip();
        String response = StandardCharsets.UTF_8.decode(responseBuffer).toString();
        assertTrue(response.contains("HTTP/1.1 400 Bad Request"), "응답 코드가 400이어야 합니다.");
    }

}