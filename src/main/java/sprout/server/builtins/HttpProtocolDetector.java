package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.server.ProtocolDetector;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class HttpProtocolDetector implements ProtocolDetector {

    private static final int HTTP_HEADER_LENGTH = 8;
    private static final Set<String> HTTP_METHODS = Set.of(
            "GET ", "POST ", "PUT ", "DELETE ", "HEAD ", "OPTIONS ", "PATCH ", "TRACE "
    );

    @Override
    public String detect(ByteBuffer buffer) throws Exception {
        // 버퍼의 현재 위치를 기록
        buffer.mark();

        // 전체 헤더 읽기 (WebSocket 감지를 위해)
        byte[] fullHeaderBytes = new byte[buffer.remaining()];
        buffer.get(fullHeaderBytes);

        // 버퍼의 위치를 원래대로 되돌림
        buffer.reset();

        String fullHeader = new String(fullHeaderBytes, StandardCharsets.UTF_8);

        // HTTP 메서드 체크
        if (!HTTP_METHODS.stream().anyMatch(fullHeader::startsWith)) {
            return "UNKNOWN";
        }

        System.out.println("full header is " + fullHeader);

        // WebSocket Upgrade 요청은 UNKNOWN 반환 (WebSocketProtocolDetector가 처리하도록)
        if (fullHeader.contains("Upgrade: websocket") ||
            fullHeader.contains("Upgrade: WebSocket")) {
            return "UNKNOWN";
        }

        return "HTTP/1.1";
    }
}
