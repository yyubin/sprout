package sprout.server.argument.builtins;

import sprout.beans.annotation.Component;
import sprout.server.argument.annotation.Payload;
import sprout.server.websocket.InvocationContext;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.LifecyclePhase;

import java.io.InputStream;
import java.lang.reflect.Parameter;

@Component
public class InputStreamPayloadArgumentResolver implements WebSocketArgumentResolver {
    @Override
    public boolean supports(Parameter parameter, InvocationContext context) {
        return  parameter.isAnnotationPresent(Payload.class) &&
                context.phase() == LifecyclePhase.MESSAGE &&
                InputStream.class.isAssignableFrom(parameter.getType()) &&
                context.session().getInputStream() != null &&
                context.getMessagePayload() == null &&
                context.isFin();
    }

    @Override
    public Object resolve(Parameter parameter, InvocationContext context) throws Exception {
        return context.getInputStream();
    }
}
