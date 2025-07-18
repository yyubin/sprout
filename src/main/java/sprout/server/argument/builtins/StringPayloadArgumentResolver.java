package sprout.server.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.server.argument.annotation.Payload;
import sprout.server.websocket.InvocationContext;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.LifecyclePhase;

import java.lang.reflect.Parameter;

@Component
public class StringPayloadArgumentResolver implements WebSocketArgumentResolver {
    @Override
    public boolean supports(Parameter parameter, InvocationContext context) {
        return parameter.isAnnotationPresent(Payload.class) &&
                context.phase() == LifecyclePhase.MESSAGE &&
                context.getMessagePayload().isText() &&
                context.getMessagePayload().asText().length() > 0 &&
                String.class.isAssignableFrom(parameter.getType());
    }

    @Override
    public Object resolve(Parameter parameter, InvocationContext context) throws Exception {
        return context.getMessagePayload().asText();
    }
}
