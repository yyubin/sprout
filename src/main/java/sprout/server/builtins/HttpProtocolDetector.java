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

        int readLimit = Math.min(buffer.remaining(), HTTP_HEADER_LENGTH);
        byte[] headerBytes = new byte[readLimit];
        buffer.get(headerBytes);

        // 버퍼의 위치를 원래대로 되돌림
        buffer.reset();

        String prefix = new String(headerBytes, StandardCharsets.UTF_8);

        if (HTTP_METHODS.stream().anyMatch(prefix::startsWith)) {
            return "HTTP/1.1";
        }

        return "UNKNOWN";
    }
}
