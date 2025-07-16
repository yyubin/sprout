package sprout.server.argument;

import sprout.server.websocket.WebSocketSession;

import java.lang.reflect.Parameter;

public interface WebSocketArgumentResolver {
    boolean supports(Parameter parameter);
    Object resolve(Parameter parameter, WebSocketSession session, String messagePayload) throws Exception;
}
