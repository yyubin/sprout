package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.server.ProtocolDetector;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class HttpProtocolDetector implements ProtocolDetector {

    private static final int HTTP_HEADER_LENGTH = 8;

    @Override
    public String detect(InputStream input) throws Exception {
        byte[] header = new byte[HTTP_HEADER_LENGTH];
        input.mark(HTTP_HEADER_LENGTH);
        int read = input.read(header, 0, HTTP_HEADER_LENGTH);
        input.reset();

        String prefix = new String(header, 0, read, StandardCharsets.UTF_8);
        if (prefix.startsWith("GET ") || prefix.startsWith("POST") || prefix.startsWith("HEAD") || prefix.startsWith("PUT") || prefix.startsWith("DELETE") || prefix.startsWith("OPTIONS") || prefix.startsWith("TRACE") || prefix.startsWith("PATCH")) {
            return "HTTP/1.1";
        }
        return "UNKNOWN";
    }
}
