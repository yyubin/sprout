package sprout.data.orm.spi;

// l2 cache
public interface Cache {
    public boolean contains(Class cls, Object primaryKey);
    public void evict(Class cls, Object primaryKey);
    public void evict(Class cls);
    public void evictAll();
    public <T> T unwrap(Class<T> cls);
}
