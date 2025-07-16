package sprout.server.websocket.handler;

import sprout.beans.annotation.Component;
import sprout.mvc.http.HttpRequest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

@Component
public class DefaultWebSocketHandshakeHandler implements WebSocketHandshakeHandler{
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    @Override
    public boolean performHandshake(HttpRequest<?> request, BufferedWriter out) throws IOException {
        // 1. 필수 헤더 검증
        Map<String, String> headers = request.getHeaders();
        String upgradeHeader = headers.get("Upgrade");
        String connectionHeader = headers.get("Connection");
        String secWebSocketKey = headers.get("Sec-WebSocket-Key");
        String secWebSocketVersion = headers.get("Sec-WebSocket-Version");

        if (!"websocket".equalsIgnoreCase(upgradeHeader) ||
                !"Upgrade".equalsIgnoreCase(connectionHeader) || // Connection 헤더는 Upgrade 포함해야 함
                secWebSocketKey == null || secWebSocketKey.isBlank() ||
                !"13".equals(secWebSocketVersion)) { // WebSocket Version 13 (RFC 6455)
            sendHandshakeErrorResponse(out, 400, "Bad Request", "Invalid WebSocket handshake request headers.");
            return false;
        }

        // 2. Sec-WebSocket-Accept 값 계산
        String secWebSocketAccept;
        try {
            secWebSocketAccept = generateSecWebSocketAccept(secWebSocketKey);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("SHA-1 algorithm not found for WebSocket handshake: " + e.getMessage());
            sendHandshakeErrorResponse(out, 500, "Internal Server Error", "Server error during handshake.");
            return false;
        }

        // 3. 핸드셰이크 성공 응답 전송
        out.write("HTTP/1.1 101 Switching Protocols\r\n");
        out.write("Upgrade: websocket\r\n");
        out.write("Connection: Upgrade\r\n");
        out.write("Sec-WebSocket-Accept: " + secWebSocketAccept + "\r\n");
        // TODO: 서브프로토콜, 확장 등 추가 헤더 처리 (선택 사항)
        // out.write("Sec-WebSocket-Protocol: " + subProtocol + "\r\n");
        out.write("\r\n"); // 헤더 끝
        out.flush();

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
    private void sendHandshakeErrorResponse(BufferedWriter out, int statusCode, String statusText, String message) throws IOException {
        out.write("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
        out.write("Content-Type: text/plain;charset=UTF-8\r\n");
        out.write("Content-Length: " + message.getBytes(StandardCharsets.UTF_8).length + "\r\n");
        out.write("\r\n");
        out.write(message);
        out.flush();
    }
}
