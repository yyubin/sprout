package sprout.server.builtins;

import sprout.server.ConnectionManager;
import sprout.server.ProtocolDetector;
import sprout.server.ProtocolHandler;
import sprout.server.ServerStrategy;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class NioServerStrategy implements ServerStrategy {

    private final ConnectionManager connectionManager;
    private volatile boolean running = true;

    public NioServerStrategy(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public void start(int port) throws Exception {
        Selector selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (running) {
            selector.select();
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();

            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();

                if (key.isAcceptable()) {
                    connectionManager.acceptConnection(key, selector);
                }
            }
        }
    }

    @Override
    public void stop() throws Exception {

    }
}
