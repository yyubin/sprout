package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.server.*;
import sprout.server.websocket.ReadableHandler;
import sprout.server.websocket.WebSocketContainer;
import sprout.server.websocket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
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
    public void start(int port) throws Exception {
        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
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

                try { // FIX: 개별 키 처리 로직을 try-catch로 감쌈
                    if (key.isAcceptable()) {
                        connectionManager.acceptConnection(key, selector);
                    }
                    if (key.isReadable()) {
                        ReadableHandler handler = (ReadableHandler) key.attachment();
                        if (handler != null) {
                            handler.read(key);
                        }
                    }
                } catch (IOException e) { // 클라이언트 연결 끊김 등 I/O 예외 처리
                    System.err.println("I/O error on connection: " + e.getMessage());
                    cleanupConnection(key);
                } catch (Exception e) { // 기타 예외 처리
                    System.err.println("Error handling key " + key + ": " + e.getMessage());
                    e.printStackTrace();
                    cleanupConnection(key);
                }
            }
        }

        selector.close();
        serverChannel.close();
    }

    @Override
    public void stop() throws Exception {
        running = false;
        if (selector != null && selector.isOpen()) {
            selector.wakeup();
        }
    }

    private void cleanupConnection(SelectionKey key) throws IOException {
        if (key.attachment() instanceof WebSocketSession) {
            WebSocketSession session = (WebSocketSession) key.attachment();
            session.close();
        }
        key.cancel();
        key.channel().close();
    }
}
