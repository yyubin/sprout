package sprout.server.websocket.message;

import sprout.server.websocket.WebSocketFrame;

import java.nio.charset.StandardCharsets;

public interface WebSocketMessageParser {
    ParsedMessage parse(String messageContent) throws Exception;
    default ParsedMessage parse(byte[] binaryContent) throws Exception {
        // 기본 구현: 바이너리 데이터를 UTF-8 문자열로 간주하고 기존 파서 호출
        return parse(new String(binaryContent, StandardCharsets.UTF_8));
    }
}
