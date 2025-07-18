package sprout.server.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.server.websocket.InvocationContext;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.LifecyclePhase;

import java.lang.reflect.Parameter;

@Component
public class ThrowableArgumentResolver implements WebSocketArgumentResolver {
    @Override
    public boolean supports(Parameter parameter, InvocationContext context) {
        return context.phase() == LifecyclePhase.ERROR &&
                Throwable.class.isAssignableFrom(parameter.getType()) &&
                context.error() != null;
    }

    @Override
    public Object resolve(Parameter parameter, InvocationContext context) throws Exception {
        return context.error();
    }
}
