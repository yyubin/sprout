package sprout.server.builtins;

import sprout.server.RequestExecutorService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RequestExecutorPoolService implements RequestExecutorService {

    private final ExecutorService pool;

    public RequestExecutorPoolService(int threadPoolSize) {
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
