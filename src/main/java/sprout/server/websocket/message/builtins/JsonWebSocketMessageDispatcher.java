package sprout.server.websocket.message.builtins;

import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.beans.annotation.Component;
import sprout.server.argument.WebSocketArgumentResolver;
import sprout.server.websocket.InvocationContext;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.WebSocketFrameDecoder;
import sprout.server.websocket.message.AbstractWebSocketMessageDispatcher;
import sprout.server.websocket.message.ParsedMessage;

import java.util.List;

@Component
public class JsonWebSocketMessageDispatcher extends AbstractWebSocketMessageDispatcher {
    private final ObjectMapper objectMapper;

    public JsonWebSocketMessageDispatcher(ObjectMapper objectMapper, List<WebSocketArgumentResolver> argumentResolvers) {
        super(argumentResolvers); // 부모 클래스에 의존성 전달
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(WebSocketFrame frame, InvocationContext context) {
        // 텍스트 프레임(opcode 0x1) 또는 텍스트 연속 프레임(opcode 0x0)만 지원
        return (frame.getOpcode() == 0x1 || frame.getOpcode() == 0x0) && context.getMessagePayload().isText();
    }

    @Override
    protected DispatchInfo prepareDispatchInfo(InvocationContext context) throws Exception {
        String messageContent = context.getMessagePayload().asText();
        ParsedMessage parsedMessage = objectMapper.readValue(messageContent, ParsedMessage.class);

        return new DispatchInfo(parsedMessage.getDestination(), parsedMessage.getPayload());
    }
}