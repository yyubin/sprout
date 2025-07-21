package sprout.context;

import java.util.Collection;
import java.util.List;

public interface BeanFactory {
    void reset();
    Object getBean(String name);
    <T> T getBean(Class<T> requiredType);
    Collection<Object> getAllBeans();
    <T> List<T> getAllBeans(Class<T> type);
    boolean containsBean(String name);
    void registerRuntimeBean(String name, Object bean);
    void registerCoreSingleton(String name, Object bean);
    CtorMeta lookupCtorMeta(Object bean);
}
