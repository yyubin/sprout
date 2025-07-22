package sprout.context;

public interface ContextPropagator<T> {
    T capture();
    void restore(T value);
    void clear();
}
