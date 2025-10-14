package sprout.server.builtins;

import sprout.server.RequestExecutorService;

import java.util.concurrent.*;

public class RequestExecutorPoolService implements RequestExecutorService {

    private final ExecutorService pool;

    public RequestExecutorPoolService(int threadPoolSize) {
        this.pool = new ThreadPoolExecutor(
                threadPoolSize,
                threadPoolSize,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(threadPoolSize * 100),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
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
