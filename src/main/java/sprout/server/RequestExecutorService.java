package sprout.server;

public interface RequestExecutorService {
    void execute(Runnable task);
    void shutdown();
}
