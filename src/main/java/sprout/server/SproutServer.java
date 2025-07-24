package sprout.server;

public interface SproutServer {
    int start(int port) throws Exception;
    void stop() throws Exception;
    boolean isRunning();
    int getPort();
}
