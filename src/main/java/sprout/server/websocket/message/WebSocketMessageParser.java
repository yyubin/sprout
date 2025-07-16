package sprout.server.websocket.message;

import sprout.server.websocket.WebSocketFrame;

public interface WebSocketMessageParser {
    String extractDestination(WebSocketFrame frame);
    String extractPayload(WebSocketFrame frame);
}
