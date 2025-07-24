package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.server.*;
import sprout.server.ReadableHandler;
import sprout.server.websocket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

@Component
public class NioHybridServerStrategy implements ServerStrategy {

    private final ConnectionManager connectionManager;
    private volatile boolean running = true;

    private Selector selector;
    private ServerSocketChannel serverChannel;

    public NioHybridServerStrategy(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    @Override
    public int start(int port) throws Exception {
        selector = Selector.open();
        serverChannel = ServerSocketChannel.open();
        serverChannel.bind(new InetSocketAddress(port));
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        running = true;
        Thread t = new Thread(this::eventLoop, "sprout-nio-loop");
        t.setDaemon(true);
        t.start();

        int actual = ((InetSocketAddress) serverChannel.getLocalAddress()).getPort();
        return actual;
    }

    private void eventLoop() {
        try {
            while (running) {
                selector.select(); // or select(timeout)
                for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext();) {
                    SelectionKey key = it.next();
                    it.remove();

                    if (!key.isValid()) { cleanupConnection(key); continue; }

                    try {
                        if (key.isAcceptable()) {
                            connectionManager.acceptConnection(key, selector);
                        }
                        Object att = key.attachment();
                        if (key.isReadable() && att instanceof ReadableHandler rh) {
                            rh.read(key);
                        }
                        if (key.isWritable() && att instanceof WritableHandler wh) {
                            wh.write(key);
                        }
                    } catch (IOException ioe) {
                        System.err.println("I/O error: " + ioe.getMessage());
                        cleanupConnection(key);
                    } catch (Exception e) {
                        e.printStackTrace();
                        cleanupConnection(key);
                    }
                }
            }
        } catch (ClosedSelectorException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { selector.close(); } catch (Exception ignored) {}
            try { serverChannel.close(); } catch (Exception ignored) {}
        }
    }

    @Override
    public void stop() throws Exception {
        running = false;
        if (selector != null) selector.wakeup();
    }

    @Override
    public boolean isRunning() {
        return running && selector != null && selector.isOpen();
    }

    private void cleanupConnection(SelectionKey key) throws IOException {
        try {
            Object att = key.attachment();
            if (att instanceof WebSocketSession ws) {
                try { ws.close(); } catch (Exception ignore) {}
            }
        } finally {
            key.cancel();
            SelectableChannel ch = key.channel();
            if (ch != null && ch.isOpen()) {
                try { ch.close(); } catch (Exception ignore) {}
            }
        }
    }
}
