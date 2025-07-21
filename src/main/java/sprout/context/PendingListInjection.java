package sprout.context;

import java.util.List;

public class PendingListInjection {
    private final Object beanInstance;
    private final Class<?> genericType;
    private final List<Object> listToPopulate;

    public PendingListInjection(Object beanInstance, List<Object> listToPopulate, Class<?> genericType) {
        this.beanInstance = beanInstance;
        this.listToPopulate = listToPopulate;
        this.genericType = genericType;
    }

    public Object getBeanInstance() {
        return beanInstance;
    }

    public Class<?> getGenericType() {
        return genericType;
    }

    public List<Object> getListToPopulate() {
        return listToPopulate;
    }
}
