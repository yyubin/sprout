package sprout.server;

import java.nio.channels.SelectionKey;

public interface ReadableProtocolHandler extends ProtocolHandler {
    void read(SelectionKey key) throws Exception;
}