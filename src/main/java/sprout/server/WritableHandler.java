package sprout.server;

import java.nio.channels.SelectionKey;

public interface WritableHandler {
    void write(SelectionKey key) throws Exception;
}
