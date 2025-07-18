package sprout.server.websocket.message;

public interface MessagePayload {
    boolean isText();
    boolean isBinary();
    String asText();
    byte[] asBinary();
}
