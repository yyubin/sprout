package sprout.server.argument.builtins;

import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.beans.annotation.Component;
import sprout.server.argument.annotation.Payload;
import sprout.server.websocket.InvocationContext;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.LifecyclePhase;
import sprout.server.websocket.message.MessagePayload;

import java.lang.reflect.Parameter;

@Component
public class JsonPayloadArgumentResolver implements WebSocketArgumentResolver {
    private final ObjectMapper objectMapper;

    public JsonPayloadArgumentResolver() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean supports(Parameter parameter, InvocationContext context){
        return parameter.isAnnotationPresent(Payload.class) &&
                context.phase() == LifecyclePhase.MESSAGE &&
                context.getMessagePayload() != null &&
                !parameter.getType().equals(String.class);
    }

    @Override
    public Object resolve(Parameter parameter, InvocationContext context) throws Exception {
        MessagePayload messagePayload = context.getMessagePayload();
        return objectMapper.readValue(messagePayload.asText(), parameter.getType());
    }
}
