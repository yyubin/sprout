package sprout.server;

import sprout.beans.annotation.Component;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.parser.HttpRequestParser;

import java.net.ServerSocket;
import java.net.Socket;

@Component
public class BioServerStrategy implements ServerStrategy{

    private final ThreadService threadService;
    private final RequestDispatcher dispatcher;
    private final HttpRequestParser parser;
    private volatile boolean running = true;

    public BioServerStrategy(ThreadService threadService, RequestDispatcher dispatcher, HttpRequestParser parser) {
        this.threadService = threadService;
        this.dispatcher = dispatcher;
        this.parser = parser;
    }

    @Override
    public void start(int port) throws Exception {
        try (ServerSocket server = new ServerSocket(port)) {
            while (running) {
                Socket socket = server.accept();
                threadService.execute(new ConnectionHandler(socket, dispatcher, parser));
            }
        }
    }

    @Override
    public void stop() throws Exception {
        running = false;
        threadService.shutdown();
    }
}
