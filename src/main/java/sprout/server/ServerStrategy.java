package sprout.server;

public interface ServerStrategy {
    int start(int port) throws Exception;
    void stop() throws Exception;
    boolean isRunning();
}
