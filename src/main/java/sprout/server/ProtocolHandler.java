package sprout.server;

import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public interface ProtocolHandler {
    boolean supports(String protocol);
}
