package sprout.server;

import sprout.beans.annotation.Component;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.parser.HttpRequestParser;

import java.net.ServerSocket;
import java.net.Socket;

@Component
public class HttpServer {

    private final ServerStrategy serverStrategy;

    public HttpServer(ServerStrategy serverStrategy) {
        this.serverStrategy = serverStrategy;
    }

    public void start(int port) throws Exception {
        serverStrategy.start(port);
    }
}