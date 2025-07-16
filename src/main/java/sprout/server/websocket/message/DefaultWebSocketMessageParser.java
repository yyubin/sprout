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
    public String extractDestination(WebSocketFrame frame) {
        if (!WebSocketFrameDecoder.isDataFrame(frame) || frame.getOpcode() != 0x1) {
            // 이 파서가 텍스트 메시지만 처리한다고 가정
            return null;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(WebSocketFrameDecoder.decodeText(frame));
            if (rootNode.has("destination") && rootNode.get("destination").isTextual()) {
                return rootNode.get("destination").asText();
            }
        } catch (IOException e) {
            System.err.println("Failed to parse WebSocket message for destination: " + e.getMessage());
        }
        return null;
    }

    @Override
    public String extractPayload(WebSocketFrame frame) {
        if (!WebSocketFrameDecoder.isDataFrame(frame) || frame.getOpcode() != 0x1) {
            return null;
        }

        try {
            // JSON 페이로드를 파싱하여 "payload" 필드의 내용을 JSON 문자열로 반환
            JsonNode rootNode = objectMapper.readTree(WebSocketFrameDecoder.decodeText(frame));
            if (rootNode.has("payload")) {
                return rootNode.get("payload").toString();
            }
        } catch (IOException e) {
            System.err.println("Failed to parse WebSocket message for payload: " + e.getMessage());
        }
        return WebSocketFrameDecoder.decodeText(frame);
    }
}
