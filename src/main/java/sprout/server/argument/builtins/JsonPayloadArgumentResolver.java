package sprout.server.argument.builtins;

import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.beans.annotation.Component;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.WebSocketSession;

import java.lang.reflect.Parameter;

@Component
public class JsonPayloadArgumentResolver implements WebSocketArgumentResolver {
    private final ObjectMapper objectMapper;

    public JsonPayloadArgumentResolver() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean supports(Parameter parameter) {
        // 단순히 JSON 파싱이 가능한 모든 객체를 지원한다고 가정
        return true; // 나중엔 @Payload 같은 애노테이션으로 제한 가능
    }

    @Override
    public Object resolve(Parameter parameter, WebSocketSession session, String messagePayload) throws Exception {
        return objectMapper.readValue(messagePayload, parameter.getType());
    }
}
