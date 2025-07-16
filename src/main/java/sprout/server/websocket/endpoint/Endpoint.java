package sprout.server.websocket.endpoint;

import sprout.server.websocket.CloseReason;
import sprout.server.websocket.WebSocketSession;

public interface Endpoint {
    public void onOpen(WebSocketSession session, EndpointConfig config);
    public void onClose(WebSocketSession session, CloseReason closeReason);

}
