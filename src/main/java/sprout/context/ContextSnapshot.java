package sprout.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

public final class ContextSnapshot{

    private final List<ContextPropagator> propagators;
    private final List<Object> captured;

    public ContextSnapshot(List<ContextPropagator> propagators) {
        this.propagators = propagators;
        this.captured = new ArrayList<>(propagators.size());
        for (ContextPropagator<?> p : propagators) {
            captured.add(p.capture());
        }
    }

    public Runnable wrap(Runnable runnable) {
        if (propagators.isEmpty()) return runnable;
        return () -> {
            restoreAll();
            try { runnable.run(); }
            finally { clearAll(); }
        };
    }

    public <T> Callable<T> wrap(Callable<T> callable) {
        if (propagators.isEmpty()) return callable;
        return () -> {
            restoreAll();
            try { return callable.call(); }
            finally { clearAll(); }
        };
    }

    @SuppressWarnings("unchecked")
    private void restoreAll() {
        for (int i = 0; i < propagators.size(); i++) {
            ((ContextPropagator<Object>) propagators.get(i)).restore(captured.get(i));
        }
    }

    private void clearAll() {
        for (ContextPropagator<?> p : propagators) p.clear();
    }
}
