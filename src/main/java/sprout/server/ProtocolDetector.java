package sprout.server;

import java.io.InputStream;

public interface ProtocolDetector {
    String detect(InputStream input) throws Exception;
}
