package sprout.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

public class MethodBeanDefinition implements BeanDefinition {
    private final String name;
    private final Class<?> type; // @Bean 메서드의 반환 타입
    private final Method factoryMethod;
    private final String factoryBeanName; // @Configuration 클래스의 빈 이름
    private final Class<?>[] factoryMethodArgumentTypes;
    private boolean isProxyTarget = false;
    private boolean primary = false;

    public MethodBeanDefinition(String name, Class<?> type, Method factoryMethod, String factoryBeanName, Class<?>[] factoryMethodArgumentTypes) {
        this.name = name;
        this.type = type;
        this.factoryMethod = factoryMethod;
        this.factoryBeanName = factoryBeanName;
        this.factoryMethodArgumentTypes = factoryMethodArgumentTypes;
    }

    @Override public String getName() { return name; }
    @Override public Class<?> getType() { return type; }
    @Override public BeanCreationMethod getCreationMethod() { return BeanCreationMethod.FACTORY_METHOD; }
    @Override public Constructor<?> getConstructor() { return null; }
    @Override public Class<?>[] getConstructorArgumentTypes() { return null; }
    @Override public Method getFactoryMethod() { return factoryMethod; }
    @Override public String getFactoryBeanName() { return factoryBeanName; }

    @Override
    public Class<?>[] getFactoryMethodArgumentTypes() {
        if (factoryMethodArgumentTypes != null) {
            return factoryMethodArgumentTypes;
        }
        // 파라미터가 없는 경우 -> 반환 타입의 주 생성자 파라미터를 의존성으로 간주
        Constructor<?> primaryCtor = Arrays.stream(type.getDeclaredConstructors())
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElse(null);

        return primaryCtor != null ? primaryCtor.getParameterTypes() : new Class<?>[0];
    }

    @Override public boolean isProxyTarget() { return isProxyTarget; }
    public void setProxyTarget(boolean proxyTarget) { isProxyTarget = proxyTarget; }

    @Override public boolean isConfigurationClassProxyNeeded() { return false; } // @Bean 메서드는 자체로 Configuration 프록시 대상이 아님

    @Override
    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}