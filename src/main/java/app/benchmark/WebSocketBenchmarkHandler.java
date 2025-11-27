package app.benchmark;

import sprout.beans.annotation.Component;
import sprout.server.argument.annotation.Payload;
import sprout.server.websocket.CloseCode;
import sprout.server.websocket.WebSocketSession;
import sprout.server.websocket.annotation.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 벤치마크용 핸들러
 *
 * 지원 기능:
 * - /echo: 메시지 에코
 * - /broadcast: 모든 연결된 클라이언트에 브로드캐스트
 * - /chat: 채팅방 시뮬레이션
 * - /ping-pong: 간단한 핑퐁 응답
 */
@Component
@WebSocketHandler("/ws/benchmark")
public class WebSocketBenchmarkHandler {

    // 연결된 모든 세션 관리
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // 통계 정보
    private static long totalMessagesReceived = 0;
    private static long totalMessagesSent = 0;
    private static long totalConnections = 0;

    @OnOpen
    public void onOpen(WebSocketSession session) {
        sessions.put(session.getId(), session);
        totalConnections++;
        System.out.println("[WebSocket Benchmark] 연결 열림: " + session.getId() +
                          " (총 연결: " + sessions.size() + ", 누적: " + totalConnections + ")");
    }

    @OnClose
    public void onClose(WebSocketSession session, CloseCode closeCode) {
        sessions.remove(session.getId());
        System.out.println("[WebSocket Benchmark] 연결 닫힘: " + session.getId() +
                          " (코드: " + closeCode.getCode() + ", 남은 연결: " + sessions.size() + ")");
    }

    @OnError
    public void onError(WebSocketSession session, Throwable error) {
        System.err.println("[WebSocket Benchmark] 에러 발생: " + session.getId() +
                          " - " + error.getMessage());
        error.printStackTrace();
    }

    @MessageMapping("/echo")
    public void handleEcho(WebSocketSession session, @Payload String message) throws IOException {
        totalMessagesReceived++;
        session.sendText(createResponse("/echo", message));
        System.out.println("[WebSocket Benchmark] Echo: " + message);
        totalMessagesSent++;
    }

    @MessageMapping("/broadcast")
    public void handleBroadcast(WebSocketSession session, @Payload String message) throws IOException {
        totalMessagesReceived++;
        String response = createResponse("/broadcast", "From " + session.getId() + ": " + message);
        System.out.println("[WebSocket Benchmark] Broadcast: " + message);
        for (WebSocketSession s : sessions.values()) {
            if (s.isOpen()) {
                try {
                    s.sendText(response);
                    totalMessagesSent++;
                } catch (IOException e) {
                    System.err.println("브로드캐스트 실패: " + s.getId() + " - " + e.getMessage());
                }
            }
        }
    }

    @MessageMapping("/chat")
    public void handleChat(WebSocketSession session, @Payload String message) throws IOException {
        totalMessagesReceived++;
        String username = (String) session.getUserProperties().get("username");
        if (username == null) {
            username = "User-" + session.getId().substring(0, 8);
            session.getUserProperties().put("username", username);
        }

        String chatMessage = username + ": " + message;
        System.out.println("[WebSocket Benchmark] Chat: " + chatMessage);
        String response = createResponse("/chat", chatMessage);

        // 채팅방의 모든 사용자에게 전송
        for (WebSocketSession s : sessions.values()) {
            if (s.isOpen()) {
                try {
                    s.sendText(response);
                    totalMessagesSent++;
                } catch (IOException e) {
                    System.err.println("채팅 메시지 전송 실패: " + s.getId());
                }
            }
        }
    }

    @MessageMapping("/ping")
    public void handlePing(WebSocketSession session, @Payload String message) throws IOException {
        totalMessagesReceived++;
        // 실제 WebSocket Ping 프레임 전송 (브라우저가 자동으로 Pong 응답)
        byte[] pingData = "ping".getBytes();
        session.sendPing(pingData);
        System.out.println("[WebSocket Benchmark] Sent Ping frame to client: " + session.getId());
        totalMessagesSent++;
    }

    @MessageMapping("/stats")
    public void handleStats(WebSocketSession session, @Payload String message) throws IOException {
        totalMessagesReceived++;
        String stats = String.format(
            "연결: %d, 수신: %d, 송신: %d, 누적 연결: %d",
            sessions.size(), totalMessagesReceived, totalMessagesSent, totalConnections
        );
        session.sendText(createResponse("/stats", stats));
        totalMessagesSent++;
    }

    private String createResponse(String destination, String payload) {
        // JSON 형식: {"destination": "...", "payload": "..."}
        return String.format("{\"destination\":\"%s\",\"payload\":\"%s\"}",
                           destination, escapeJson(payload));
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public static void resetStats() {
        totalMessagesReceived = 0;
        totalMessagesSent = 0;
        totalConnections = 0;
    }

    public static String getStats() {
        return String.format(
            "Sessions: %d, Received: %d, Sent: %d, Total Connections: %d",
            sessions.size(), totalMessagesReceived, totalMessagesSent, totalConnections
        );
    }
}
