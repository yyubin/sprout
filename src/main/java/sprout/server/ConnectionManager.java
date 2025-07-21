package sprout.server;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public interface ConnectionManager {
    void acceptConnection(SelectionKey selectionKey, Selector Selector) throws Exception;
}
