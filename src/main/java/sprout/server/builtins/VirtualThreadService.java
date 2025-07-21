package sprout.server.builtins;

import sprout.context.ContextPropagator;
import sprout.context.ContextSnapshot;
import sprout.mvc.dispatcher.RequestDispatcher;
import sprout.server.ConnectionHandler;
import sprout.server.ThreadService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VirtualThreadService implements ThreadService {

    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
    private final List<ContextPropagator> propagators;

    public VirtualThreadService(List<ContextPropagator> propagators) {
        this.propagators = propagators;
    }

    @Override
    public void execute(Runnable task) {
        final ContextSnapshot snapshot = new ContextSnapshot(propagators);
        pool.execute(snapshot.wrap(task));
    }

    @Override
    public void shutdown() {
        pool.shutdown();
    }
}
