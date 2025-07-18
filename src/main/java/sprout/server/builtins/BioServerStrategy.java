package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.parser.HttpRequestParser;
import sprout.server.ConnectionHandler;
import sprout.server.ConnectionManager;
import sprout.server.ServerStrategy;
import sprout.server.ThreadService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class BioServerStrategy implements ServerStrategy {

    private final ThreadService threadService;
    private final ConnectionManager connectionManager;
    private volatile boolean running = true;

    public BioServerStrategy(ThreadService threadService, ConnectionManager connectionManager) {
        this.threadService = threadService;
        this.connectionManager = connectionManager;
    }

    @Override
    public void start(int port) throws Exception {
        try (ServerSocket server = new ServerSocket(port)) {
            while (running) {
                Socket socket = server.accept();
                threadService.execute(() -> {
                    try {
                        connectionManager.handleConnection(socket);
                    } catch (Exception e) {
                        System.err.println("Error handling connection: " + e.getMessage());
                        e.printStackTrace();
                        try {
                            if (!socket.isClosed()) socket.close();
                        } catch (IOException ex) { /* ignore */ }
                    }
                });
            }
        }
    }

    @Override
    public void stop() throws Exception {
        running = false;
        threadService.shutdown();
    }
}
