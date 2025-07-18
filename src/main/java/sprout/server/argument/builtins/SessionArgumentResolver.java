package sprout.server.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.server.websocket.InvocationContext;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.argument.annotation.SocketSession;
import sprout.server.websocket.WebSocketSession;

import java.lang.reflect.Parameter;

@Component
public class SessionArgumentResolver implements WebSocketArgumentResolver {
    @Override
    public boolean supports(Parameter parameter, InvocationContext context) {
        return (WebSocketSession.class.isAssignableFrom(parameter.getType()) && context.session() != null) || (parameter.isAnnotationPresent(SocketSession.class) && context.session() != null);
    }

    @Override
    public Object resolve(Parameter parameter, InvocationContext context) {
        return context.session();
    }
}
