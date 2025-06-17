package sprout.server;

import sprout.beans.annotation.Component;
import sprout.beans.annotation.Requires;
import sprout.mvc.dispatcher.RequestDispatcher;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class HttpServer {

    private final ExecutorService pool = Executors.newFixedThreadPool(16);
    private final RequestDispatcher dispatcher;

    public HttpServer(RequestDispatcher dispatcher) { this.dispatcher = dispatcher; }

    public void start(int port) throws Exception {
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                Socket conn = server.accept();
                pool.execute(new ConnectionHandler(conn, dispatcher));
            }
        }
    }
}