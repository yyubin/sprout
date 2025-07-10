package sprout.server;

import sprout.beans.annotation.Component;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.parser.HttpRequestParser;

import java.net.ServerSocket;
import java.net.Socket;

@Component
public class HttpServer {

    private final ThreadService threadService;
    private final RequestDispatcher dispatcher;
    private final HttpRequestParser parser;

    public HttpServer(ThreadService threadService, RequestDispatcher dispatcher, HttpRequestParser parser) {
        this.threadService = threadService;
        this.dispatcher = dispatcher;
        this.parser = parser;
    }

    public void start(int port) throws Exception {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (true) {
                Socket socket = server.accept();
                ConnectionHandler handler = new ConnectionHandler(socket, dispatcher, parser);
                threadService.execute(handler);
            }
        } finally {
            threadService.shutdown();
        }
    }
}