package sprout.data.orm.spi;

import java.util.Map;

public interface EntityManagerFactory extends AutoCloseable{
    public EntityManager createEntityManager();
    public EntityManager createEntityManager(Map map);
    public boolean isOpen();
    public void close();
    public Map<String, Object> getProperties();
}
