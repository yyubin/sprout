package sprout.data.orm.spi;

public interface EntityTransaction {
    void begin();
    void commit();
    void rollback();
    boolean isActive();
}
