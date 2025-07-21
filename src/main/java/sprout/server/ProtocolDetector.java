package sprout.server;

import java.io.InputStream;
import java.nio.ByteBuffer;

public interface ProtocolDetector {
    String detect(ByteBuffer buffer) throws Exception;
}
