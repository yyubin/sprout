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

    private final ThreadService threadService;
    private final RequestDispatcher dispatcher;

    public HttpServer(ThreadService threadService, RequestDispatcher dispatcher) {
        this.threadService = threadService;
        this.dispatcher = dispatcher;
    }

    public void start(int port) throws Exception {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (true) {
                Socket socket = server.accept();
                ConnectionHandler handler = new ConnectionHandler(socket, dispatcher);
                threadService.execute(handler);
            }
        } finally {
            threadService.shutdown();
        }
    }
}