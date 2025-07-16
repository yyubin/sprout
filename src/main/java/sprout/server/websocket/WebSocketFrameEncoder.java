package sprout.server.websocket;

public interface WebSocketFrameEncoder {
    byte[] encodeText(String message);
}
