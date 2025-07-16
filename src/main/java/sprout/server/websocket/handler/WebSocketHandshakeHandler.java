package sprout.server.websocket.handler;

import sprout.mvc.http.HttpRequest;

import java.io.BufferedWriter;
import java.io.IOException;

public interface WebSocketHandshakeHandler {
    boolean performHandshake(HttpRequest<?> request, BufferedWriter out) throws IOException;
}
