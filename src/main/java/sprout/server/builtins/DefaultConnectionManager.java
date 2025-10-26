package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.server.*;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;

@Component
public class DefaultConnectionManager implements ConnectionManager {

    private final List<ProtocolDetector> detectors;
    private final List<ProtocolHandler> handlers;
    private final ByteBufferPool bufferPool;

    public DefaultConnectionManager(List<ProtocolDetector> detectors, List<ProtocolHandler> handlers, ByteBufferPool bufferPool) {
        this.detectors = detectors;
        this.handlers = handlers;
        this.bufferPool = bufferPool;
    }

    @Override
    public void acceptConnection(SelectionKey selectionKey, Selector selector) throws Exception {
        ServerSocketChannel serverChannel = (ServerSocketChannel) selectionKey.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);

        ByteBuffer buffer = bufferPool.acquire(ByteBufferPool.SMALL_BUFFER_SIZE);
        int bytesRead = clientChannel.read(buffer);

        if (bytesRead <= 0) {
            bufferPool.release(buffer);
            clientChannel.close();
            return;
        }

        buffer.flip();
        String detectedProtocol = "UNKNOWN";

        for (ProtocolDetector detector : detectors) {
            try {
                detectedProtocol = detector.detect(buffer);
                if (!"UNKNOWN".equals(detectedProtocol) && detectedProtocol != null) {
                    break;
                }
            } catch (Exception e) {
                System.err.println("Error detecting protocol: " + e.getMessage());
            }
        }

        if ("UNKNOWN".equals(detectedProtocol) || detectedProtocol == null) {
            System.err.println("Unknown protocol detected. Closing socket: " + clientChannel.socket());
            bufferPool.release(buffer);
            clientChannel.close();
            return;
        }

        for (ProtocolHandler handler : handlers) {
            if (handler.supports(detectedProtocol)) {
                if (handler instanceof AcceptableProtocolHandler) {
                    ((AcceptableProtocolHandler) handler).accept(clientChannel, selector, buffer);
                    return; // 핸들러를 찾았으므로 종료
                }

            }
        }

        bufferPool.release(buffer);
    }
}
