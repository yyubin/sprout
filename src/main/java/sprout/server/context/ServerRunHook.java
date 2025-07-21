package sprout.server.context;

import java.nio.channels.SelectionKey;

public interface ServerRunHook {
    void beforeServerRun(SelectionKey key) throws Exception;
    void afterServerRun(SelectionKey key) throws Exception;
}
