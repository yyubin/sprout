package sprout.beans;

import java.lang.reflect.Constructor;

public record BeanDefinition(
        Class<?> type,
        Constructor<?> constructor,
        Class<?>[] dependencies,
        boolean proxyNeeded
) {

    public BeanDefinition(Class<?> type, boolean proxyNeeded) throws NoSuchMethodException {
        this(type, type.getDeclaredConstructor(), new Class<?>[0], proxyNeeded);
    }
}