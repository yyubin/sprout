package sprout.server.websocket;

import java.io.*;
import java.nio.charset.StandardCharsets;

public final class WebSocketUtils {
    private WebSocketUtils() {
    }

    public static void readTextToBuffer(InputStream in, StringBuilder sb) throws IOException {
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        char[] buf = new char[1024];
        int len;
        while ((len = reader.read(buf)) != -1) {
            sb.append(buf, 0, len);
        }
    }

    public static void readBinaryToBuffer(InputStream in, ByteArrayOutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }

    public static void consumeTextFragment(InputStream in, StringBuilder sb) throws IOException {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            char[] buf = new char[1024];
            int len;
            while ((len = reader.read(buf)) != -1) {
                sb.append(buf, 0, len);
            }
        }
    }

    public static void consumeBinaryFragment(InputStream in, ByteArrayOutputStream out) throws IOException {
        try (InputStream fragmentStream = in) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = fragmentStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }
}
