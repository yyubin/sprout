package sprout.server.builtins;

import sprout.beans.annotation.Component;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.server.ConnectionHandler;
import sprout.server.ThreadService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ThreadPoolService implements ThreadService {

    private final ExecutorService pool;

    public ThreadPoolService(int threadPoolSize) {
        this.pool = Executors.newFixedThreadPool(threadPoolSize);
    }

    @Override
    public void execute(Runnable task) {
        pool.execute(task);
    }

    @Override
    public void shutdown() {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
