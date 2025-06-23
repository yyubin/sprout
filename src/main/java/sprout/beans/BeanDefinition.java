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
    // List<AdviceDefinition> getAdvices(); // 또는 어떤 Advice들이 적용될지 리스트로 가질 수도 있음

    // Configuration 클래스 프록시 여부
    boolean isConfigurationClassProxyNeeded();
}