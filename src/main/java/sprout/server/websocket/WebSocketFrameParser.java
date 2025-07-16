package sprout.server.websocket;

import java.io.InputStream;

public interface WebSocketFrameParser {
    WebSocketFrame parse(InputStream in) throws Exception;
}
