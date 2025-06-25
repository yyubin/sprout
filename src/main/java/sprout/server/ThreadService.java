package sprout.server;

import sprout.mvc.dispatcher.RequestDispatcher;

import java.io.IOException;
import java.net.ServerSocket;

public interface ThreadService {
    void execute(Runnable task);
    void shutdown();
}
