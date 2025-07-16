package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.server.ProtocolDetector;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class WebSocketProtocolDetector implements ProtocolDetector {

    private static final int MAX_HEADER_READ_LENGTH = 2048;

    @Override
    public String detect(InputStream input) throws Exception {
        if (!input.markSupported()) {
            input = new BufferedInputStream(input); // 내부적으로 래핑
        }

        input.mark(MAX_HEADER_READ_LENGTH);
        byte[] buffer = new byte[MAX_HEADER_READ_LENGTH];
        int readBytes = input.read(buffer, 0, MAX_HEADER_READ_LENGTH);
        input.reset();

        if (readBytes == -1) {
            return "UNKNOWN";
        }

        String fullHeader = new String(buffer, 0, readBytes, StandardCharsets.UTF_8);

        if (fullHeader.contains("\r\nUpgrade: websocket\r\n") || fullHeader.contains("\nUpgrade: websocket\n")) {
            return "WEBSOCKET";
        }
        return "UNKNOWN";
    }
}
