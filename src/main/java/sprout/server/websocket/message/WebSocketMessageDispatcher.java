package sprout.server.websocket.message;

import sprout.server.websocket.InvocationContext;
import sprout.server.websocket.WebSocketFrame;

public interface WebSocketMessageDispatcher {
    boolean supports(WebSocketFrame frame, InvocationContext context);
    boolean dispatch(WebSocketFrame frame, InvocationContext context) throws Exception;
}
