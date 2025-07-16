package sprout.server.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.WebSocketSession;

import java.lang.reflect.Parameter;

@Component
public class SessionArgumentResolver implements WebSocketArgumentResolver {
    @Override
    public boolean supports(Parameter parameter) {
        return WebSocketSession.class.isAssignableFrom(parameter.getType());
    }

    @Override
    public Object resolve(Parameter parameter, WebSocketSession session, String messagePayload) {
        return session;
    }
}
