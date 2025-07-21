package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.server.ProtocolDetector;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Component
public class WebSocketProtocolDetector implements ProtocolDetector {

    @Override
    public String detect(ByteBuffer buffer) throws Exception {
        buffer.mark();

        byte[] headerBytes = new byte[buffer.remaining()];
        buffer.get(headerBytes);

        buffer.reset();

        String fullHeader = new String(headerBytes, StandardCharsets.UTF_8);

        if (fullHeader.contains("\r\nUpgrade: websocket\r\n") || fullHeader.contains("\nUpgrade: websocket\n")) {
            return "WEBSOCKET";
        }
        return "UNKNOWN";
    }
}
