package sprout.server.websocket;

public interface CloseListener {
    void onSessionClosed(WebSocketSession session);
}
