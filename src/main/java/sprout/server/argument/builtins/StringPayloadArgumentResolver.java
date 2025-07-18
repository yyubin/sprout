package sprout.server.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.server.websocket.InvocationContext;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.LifecyclePhase;

import java.lang.reflect.Parameter;

@Component
public class StringPayloadArgumentResolver implements WebSocketArgumentResolver {
    @Override
    public boolean supports(Parameter parameter, InvocationContext context) {
        return context.phase() == LifecyclePhase.MESSAGE &&
                context.payload() != null &&
                String.class.isAssignableFrom(parameter.getType());
    }

    @Override
    public Object resolve(Parameter parameter, InvocationContext context) throws Exception {
        return context.payload();
    }
}
