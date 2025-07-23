package sprout.server.websocket;


import sprout.mvc.http.HttpRequest;
import sprout.server.ReadableHandler;
import sprout.server.websocket.endpoint.WebSocketEndpointInfo;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface WebSocketSession extends ReadableHandler {
    String getId();

    HttpRequest<?> getHandshakeRequest();

    void sendText(String message) throws IOException;
    void close() throws IOException;
    void sendBinary(byte[] data) throws IOException;
    void sendPing(byte[] data) throws IOException;
    void sendPong(byte[] data) throws IOException;
    boolean isOpen();
    Map<String, Object> getUserProperties();

    void callOnErrorMethod(Throwable error) throws Exception;

    void callOnCloseMethod(CloseCode closeCode) throws Exception;

    String getRequestPath();

    Map<String, List<String>> getRequestParameterMap();

    String getQueryString();

    Map<String, String> getPathParameters();

    void callOnOpenMethod() throws Exception;

    WebSocketEndpointInfo getEndpointInfo();
}
