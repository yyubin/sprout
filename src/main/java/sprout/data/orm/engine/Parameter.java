package sprout.data.orm.engine;

public class Parameter<T> {
    private final int index;
    private final T value;
    private final Class<T> type;

    public Parameter(int index, T value, Class<T> type) {
        this.index = index;
        this.value = value;
        this.type = type;
    }

    public int getIndex() {
        return index;
    }

    public T getValue() {
        return value;
    }

    public Class<T> getType() {
        return type;
    }
}
