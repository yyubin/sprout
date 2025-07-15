package sprout.server;

public interface ServerStrategy {
    void start(int port) throws Exception;
    void stop() throws Exception;
}
