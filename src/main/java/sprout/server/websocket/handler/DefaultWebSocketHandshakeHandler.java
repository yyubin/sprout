package sprout.server.websocket.handler;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

@Component
public class DefaultWebSocketHandshakeHandler implements WebSocketHandshakeHandler{
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    @Override
    public boolean performHandshake(HttpRequest<?> request, SocketChannel channel) throws IOException {
        // 1. 필수 헤더 검증
        Map<String, String> headers = request.getHeaders();
        String upgradeHeader = headers.get("Upgrade");
        String connectionHeader = headers.get("Connection");
        String secWebSocketKey = headers.get("Sec-WebSocket-Key");
        String secWebSocketVersion = headers.get("Sec-WebSocket-Version");

        System.out.println(upgradeHeader + ", " + connectionHeader + ", " + secWebSocketKey + ", " + secWebSocketVersion + " : " + request.getPath());

        // Connection 헤더는 "Upgrade"를 포함해야 함 (쉼표로 구분된 여러 값 가능)
        boolean hasUpgradeConnection = connectionHeader != null &&
                                       connectionHeader.toLowerCase().contains("upgrade");

        if (!"websocket".equalsIgnoreCase(upgradeHeader) ||
                !hasUpgradeConnection ||
                secWebSocketKey == null || secWebSocketKey.isBlank() ||
                !"13".equals(secWebSocketVersion)) { // WebSocket Version 13 (RFC 6455)
            sendHandshakeErrorResponse(channel, 400, "Bad Request", "Invalid WebSocket handshake request headers.");
            return false;
        }

        // 2. Sec-WebSocket-Accept 값 계산
        String secWebSocketAccept;
        try {
            secWebSocketAccept = generateSecWebSocketAccept(secWebSocketKey);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-1 algorithm not found for WebSocket handshake: " + e.getMessage());
            sendHandshakeErrorResponse(channel, 500, "Internal Server Error", "Server error during handshake.");
            return false;
        }

        // 3. 핸드셰이크 성공 응답 전송
        String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                         "Upgrade: websocket\r\n" +
                         "Connection: Upgrade\r\n" +
                         "Sec-WebSocket-Accept: " + secWebSocketAccept + "\r\n" +
                         "\r\n";

        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }

        System.out.println("WebSocket handshake successful for path: " + request.getPath());
        return true;
    }

    // Sec-WebSocket-Accept 값을 계산하는 헬퍼 메서드
    private String generateSecWebSocketAccept(String secWebSocketKey) throws NoSuchAlgorithmException {
        String combined = secWebSocketKey + WEBSOCKET_GUID;
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(combined.getBytes(StandardCharsets.US_ASCII)); // ASCII로 인코딩
        return Base64.getEncoder().encodeToString(sha1Hash);
    }

    // 핸드셰이크 실패 시 HTTP 에러 응답 전송
    private void sendHandshakeErrorResponse(SocketChannel channel, int statusCode, String statusText, String message) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                         "Content-Type: text/plain;charset=UTF-8\r\n" +
                         "Content-Length: " + message.getBytes(StandardCharsets.UTF_8).length + "\r\n" +
                         "\r\n" +
                         message;

        ByteBuffer buffer = ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8));
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }
}
