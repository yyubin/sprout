package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.server.ProtocolDetector;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class HttpProtocolDetector implements ProtocolDetector {

    private static final int HTTP_HEADER_LENGTH = 8;
    private static final Set<String> HTTP_METHODS = Set.of(
            "GET ", "POST ", "PUT ", "DELETE ", "HEAD ", "OPTIONS ", "PATCH ", "TRACE "
    );

    @Override
    public String detect(InputStream input) throws Exception {
        input.mark(HTTP_HEADER_LENGTH);
        byte[] header = new byte[HTTP_HEADER_LENGTH];
        int read = input.read(header, 0, HTTP_HEADER_LENGTH);
        input.reset();

        if (read == -1) {
            return "UNKNOWN";
        }

        String prefix = new String(header, 0, read, StandardCharsets.UTF_8);

        if (HTTP_METHODS.stream().anyMatch(prefix::startsWith)) {
            return "HTTP/1.1";
        }

        return "UNKNOWN";
    }
}
