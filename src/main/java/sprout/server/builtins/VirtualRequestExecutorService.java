package sprout.server.builtins;

import sprout.context.ContextPropagator;
import sprout.context.ContextSnapshot;
import sprout.server.RequestExecutorService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualRequestExecutorService implements RequestExecutorService {

    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
    private final List<ContextPropagator> propagators;

    public VirtualRequestExecutorService(List<ContextPropagator> propagators) {
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
