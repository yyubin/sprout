package sprout.context;

public interface ContextPropagator {
    void capture();
    void restore();
    void clear();
}
