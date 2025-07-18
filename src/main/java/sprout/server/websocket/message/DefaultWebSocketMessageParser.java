package sprout.server.websocket.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import sprout.beans.annotation.Component;
import sprout.server.websocket.WebSocketFrame;
import sprout.server.websocket.WebSocketFrameDecoder;

import java.io.IOException;

@Component
public class DefaultWebSocketMessageParser implements WebSocketMessageParser{
    private final ObjectMapper objectMapper;

    public DefaultWebSocketMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ParsedMessage parse(String messageContent) throws Exception {
        JsonNode rootNode = objectMapper.readTree(messageContent);
        if (rootNode.has("destination") && rootNode.get("destination").isTextual() &&
            rootNode.has("payload")
            ) {
            return new ParsedMessage(rootNode.get("destination").asText(), rootNode.get("payload").toString());
        }
        throw new IOException("Invalid WebSocket message: " + messageContent);
    }

}
