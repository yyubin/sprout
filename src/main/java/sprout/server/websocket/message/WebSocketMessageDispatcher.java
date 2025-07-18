package sprout.server.websocket.message;

import sprout.server.websocket.DispatchResult;
import sprout.server.websocket.InvocationContext;
import sprout.server.websocket.WebSocketFrame;

public interface WebSocketMessageDispatcher {
    boolean supports(WebSocketFrame frame, InvocationContext context);
    DispatchResult dispatch(WebSocketFrame frame, InvocationContext context) throws Exception;
}
