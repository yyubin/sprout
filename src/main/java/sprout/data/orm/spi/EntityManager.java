package sprout.data.orm.spi;

import java.util.List;
import java.util.Map;

public interface EntityManager extends AutoCloseable{
    void persist(Object entity);
    <T> T merge(T entity);
    void remove(Object entity);
    <T> T find(Class<T> entityClass, Object primaryKey);
    <T> T find(Class<T> entityClass, Object primaryKey,
               Map<String, Object> properties);
    <T> T find(Class<T> entityClass, Object primaryKey,
               LockModeType lockMode);
    public <T> T find(Class<T> entityClass, Object primaryKey,
                      LockModeType lockMode,
                      Map<String, Object> properties);
    public <T> T getReference(Class<T> entityClass,
                              Object primaryKey);
    public void flush();
    public void setFlushMode(FlushModeType flushMode);
    public FlushModeType getFlushMode();
    public void lock(Object entity, LockModeType lockMode);
    public void lock(Object entity, LockModeType lockMode,
                     Map<String, Object> properties);
    public void refresh(Object entity);
    public void refresh(Object entity,
                        Map<String, Object> properties);
    public void refresh(Object entity, LockModeType lockMode);
    public void refresh(Object entity, LockModeType lockMode,
                        Map<String, Object> properties);

    Query createQuery(String qlString);
    <T> TypedQuery<T> createQuery(String qlString, Class<T> resultClass);
    Query createNativeQuery(String sqlString);


    public void clear();
    public void detach(Object entity);


    public void joinTransaction();
    public boolean isJoinedToTransaction();


    public void close();
    public boolean isOpen();



    public EntityTransaction getTransaction();
    public EntityManagerFactory getEntityManagerFactory();
    public <T> EntityGraph<T> createEntityGraph(Class<T> rootType);
    public EntityGraph<?> createEntityGraph(String graphName);
    public  EntityGraph<?> getEntityGraph(String graphName);
    public <T> List<EntityGraph<? super T>> getEntityGraphs(Class<T> entityClass);
}
