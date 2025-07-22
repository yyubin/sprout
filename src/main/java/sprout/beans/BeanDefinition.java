package sprout.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public interface BeanDefinition {

    String getName();
    Class<?> getType();

    BeanCreationMethod getCreationMethod(); // ENUM: CONSTRUCTOR, FACTORY_METHOD

    Constructor<?> getConstructor();
    Class<?>[] getConstructorArgumentTypes();

    Method getFactoryMethod();
    String getFactoryBeanName();
    Class<?>[] getFactoryMethodArgumentTypes();

    // AOP 프록시 여부
    boolean isProxyTarget();

    // Configuration 클래스 프록시 여부
    boolean isConfigurationClassProxyNeeded();

    public boolean isPrimary();
}