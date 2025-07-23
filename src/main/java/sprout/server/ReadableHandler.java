package sprout.server;

import java.nio.channels.SelectionKey;

public interface ReadableHandler {
    void read(SelectionKey key) throws Exception;
}
