package sprout.server.websocket.handler;

import sprout.mvc.http.HttpRequest;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface WebSocketHandshakeHandler {
    boolean performHandshake(HttpRequest<?> request, SocketChannel channel) throws IOException;
}
