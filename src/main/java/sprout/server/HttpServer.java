package sprout.server;

import sprout.beans.annotation.Component;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.mvc.http.parser.HttpRequestParser;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class HttpServer implements SproutServer{

    private final ServerStrategy serverStrategy;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile int port = -1;

    public HttpServer(ServerStrategy serverStrategy) {
        this.serverStrategy = serverStrategy;
    }

    @Override
    public int start(int port) throws Exception {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("HttpServer already started");
        }
        try {
            int bound = serverStrategy.start(port);
            this.port = bound;
            return bound;
        } catch (Exception e) {
            running.set(false);
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        if (!running.compareAndSet(true, false)) return; // 이미 멈춘 상태면 무시
        try {
            serverStrategy.stop();
        } finally {
            port = -1;
        }
    }

    @Override
    public boolean isRunning() {
        return running.get() && serverStrategy.isRunning();
    }

    @Override
    public int getPort() {
        if (!running.get()) throw new IllegalStateException("Server not running");
        return port;
    }
}