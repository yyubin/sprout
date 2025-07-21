package sprout.server;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public interface AcceptableProtocolHandler extends ProtocolHandler{
    void accept(SocketChannel channel, Selector selector, ByteBuffer byteBuffer) throws Exception;
}
