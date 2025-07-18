package sprout.server.websocket;


import sprout.mvc.http.HttpRequest;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface WebSocketSession {
    String getId();

    HttpRequest<?> getHandshakeRequest();

    void sendText(String message) throws IOException;
    void close() throws IOException;
    void sendBinary(byte[] data) throws IOException;
    void sendPing(byte[] data) throws IOException;
    void sendPong(byte[] data) throws IOException;
    boolean isOpen();
    Map<String, Object> getUserProperties();

    void startMessageLoop() throws Exception;

    void callOnErrorMethod(Throwable error) throws Exception;

    void callOnCloseMethod(CloseCode closeCode) throws Exception;

    String getRequestPath();

    Map<String, List<String>> getRequestParameterMap();

    String getQueryString();

    Map<String, String> getPathParameters();

    // MessageHandler 관련 메서드는 JSR-356의 Session 인터페이스에 있으나
    // 현재 Sprout는 @MessageMapping 기반이므로 직접 구현하지 않아도 될 수 있음
    Set<MessageHandler> getMessageHandlers();

    void addMessageHandler(MessageHandler handler);

    void removeMessageHandler(MessageHandler handler);

    void callOnOpenMethod() throws Exception;

    WebSocketEndpointInfo getEndpointInfo();

    InputStream getInputStream();
}
