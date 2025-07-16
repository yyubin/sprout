package sprout.server.websocket;

import java.util.Collection;

public interface WebSocketContainer {
    void addSession(String path, WebSocketSession session);
    void removeSession(String path, String sessionId);
    Collection<WebSocketSession> getSessions(String path);
    WebSocketSession getSession(String sessionId);
}
