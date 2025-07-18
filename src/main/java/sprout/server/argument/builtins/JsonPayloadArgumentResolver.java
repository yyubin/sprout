package sprout.server.argument.builtins;

import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.beans.annotation.Component;
import sprout.server.websocket.InvocationContext;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.LifecyclePhase;

import java.lang.reflect.Parameter;

@Component
public class JsonPayloadArgumentResolver implements WebSocketArgumentResolver {
    private final ObjectMapper objectMapper;

    public JsonPayloadArgumentResolver() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean supports(Parameter parameter, InvocationContext context){
        return context.phase() == LifecyclePhase.MESSAGE &&
                context.payload() != null &&
                !parameter.getType().equals(String.class);
    }

    @Override
    public Object resolve(Parameter parameter, InvocationContext context) throws Exception {
        String messagePayload = context.payload();
        return objectMapper.readValue(messagePayload, parameter.getType());
    }
}
