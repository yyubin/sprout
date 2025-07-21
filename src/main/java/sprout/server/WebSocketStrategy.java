package sprout.server;

import sprout.mvc.http.HttpRequest;

import java.net.Socket;

public interface WebSocketStrategy {
    void handleWebSocketUpgrade(Socket socket, HttpRequest<?> request);
}
