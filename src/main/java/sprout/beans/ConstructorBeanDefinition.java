package sprout.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ConstructorBeanDefinition implements BeanDefinition {
    private final String name;
    private final Class<?> type;
    private final Constructor<?> constructor;
    private final Class<?>[] constructorArgumentTypes;
    private final Object[] constructorArguments;
    private boolean isProxyTarget = false; // AOP
    private boolean isConfigurationClassProxyNeeded = false; // @Configuration

    public ConstructorBeanDefinition(String name, Class<?> type, Constructor<?> constructor, Class<?>[] constructorArgumentTypes) {
        this.name = name;
        this.type = type;
        this.constructor = constructor;
        this.constructorArgumentTypes = constructorArgumentTypes;
        this.constructorArguments = null;
    }

    public ConstructorBeanDefinition(String name, Class<?> type, Constructor<?> constructor, Class<?>[] constructorArgumentTypes, Object[] constructorArguments) {
        this.name = name;
        this.type = type;
        this.constructor = constructor;
        this.constructorArgumentTypes = constructorArgumentTypes;
        this.constructorArguments = constructorArguments;
    }

    public Object[] getConstructorArguments() {
        return constructorArguments;
    }

    @Override public String getName() { return name; }
    @Override public Class<?> getType() { return type; }
    @Override public BeanCreationMethod getCreationMethod() { return BeanCreationMethod.CONSTRUCTOR; }
    @Override public Constructor<?> getConstructor() { return constructor; }
    @Override public Class<?>[] getConstructorArgumentTypes() { return constructorArgumentTypes; }
    @Override public Method getFactoryMethod() { return null; }
    @Override public String getFactoryBeanName() { return null; }
    @Override public Class<?>[] getFactoryMethodArgumentTypes() { return null; }

    @Override public boolean isProxyTarget() { return isProxyTarget; }
    public void setProxyTarget(boolean proxyTarget) { isProxyTarget = proxyTarget; }

    @Override public boolean isConfigurationClassProxyNeeded() { return isConfigurationClassProxyNeeded; }
    public void setConfigurationClassProxyNeeded(boolean configProxyNeeded) { isConfigurationClassProxyNeeded = configProxyNeeded; }
}