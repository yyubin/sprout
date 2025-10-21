package sprout.beans.instantiation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.context.BeanFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConstructorBasedInstantiationStrategyTest {

    private ConstructorBasedInstantiationStrategy strategy;
    private DependencyResolver mockResolver;
    private BeanFactory mockBeanFactory;

    // 테스트용 클래스들
    static class SimpleService {
        public SimpleService() {}
    }

    static class ServiceWithDependency {
        private final SimpleService simpleService;

        public ServiceWithDependency(SimpleService simpleService) {
            this.simpleService = simpleService;
        }

        public SimpleService getSimpleService() {
            return simpleService;
        }
    }

    static class PrivateConstructorService {
        private PrivateConstructorService() {}

        public static PrivateConstructorService create() {
            return new PrivateConstructorService();
        }
    }

    @BeforeEach
    void setUp() {
        strategy = new ConstructorBasedInstantiationStrategy();
        mockResolver = mock(DependencyResolver.class);
        mockBeanFactory = mock(BeanFactory.class);
    }

    @Test
    @DisplayName("CONSTRUCTOR 생성 방식을 지원한다")
    void supports_constructor() {
        // when & then
        assertTrue(strategy.supports(BeanCreationMethod.CONSTRUCTOR));
    }

    @Test
    @DisplayName("FACTORY_METHOD 생성 방식을 지원하지 않는다")
    void supports_factoryMethod() {
        // when & then
        assertFalse(strategy.supports(BeanCreationMethod.FACTORY_METHOD));
    }

    @Test
    @DisplayName("인자가 없는 생성자로 빈을 생성한다")
    void instantiate_noArgConstructor() throws Exception {
        // given
        Constructor<SimpleService> constructor = SimpleService.class.getDeclaredConstructor();
        BeanDefinition def = createMockBeanDefinition(
                "simpleService",
                SimpleService.class,
                constructor,
                new Class<?>[0],
                false
        );

        when(mockResolver.resolve(any(), any(), any())).thenReturn(new Object[0]);

        // when
        Object result = strategy.instantiate(def, mockResolver, mockBeanFactory);

        // then
        assertNotNull(result);
        assertInstanceOf(SimpleService.class, result);
    }

    @Test
    @DisplayName("의존성이 있는 생성자로 빈을 생성한다")
    void instantiate_withDependencies() throws Exception {
        // given
        Constructor<ServiceWithDependency> constructor =
            ServiceWithDependency.class.getDeclaredConstructor(SimpleService.class);

        SimpleService dependency = new SimpleService();
        BeanDefinition def = createMockBeanDefinition(
                "serviceWithDep",
                ServiceWithDependency.class,
                constructor,
                new Class<?>[]{SimpleService.class},
                false
        );

        when(mockResolver.resolve(
                eq(new Class<?>[]{SimpleService.class}),
                any(Parameter[].class),
                eq(def)
        )).thenReturn(new Object[]{dependency});

        // when
        Object result = strategy.instantiate(def, mockResolver, mockBeanFactory);

        // then
        assertNotNull(result);
        assertInstanceOf(ServiceWithDependency.class, result);
        ServiceWithDependency service = (ServiceWithDependency) result;
        assertSame(dependency, service.getSimpleService());
    }

    @Test
    @DisplayName("private 생성자도 접근하여 빈을 생성한다")
    void instantiate_privateConstructor() throws Exception {
        // given
        Constructor<PrivateConstructorService> constructor =
            PrivateConstructorService.class.getDeclaredConstructor();

        BeanDefinition def = createMockBeanDefinition(
                "privateService",
                PrivateConstructorService.class,
                constructor,
                new Class<?>[0],
                false
        );

        when(mockResolver.resolve(any(), any(), any())).thenReturn(new Object[0]);

        // when
        Object result = strategy.instantiate(def, mockResolver, mockBeanFactory);

        // then
        assertNotNull(result);
        assertInstanceOf(PrivateConstructorService.class, result);
    }

    @Test
    @DisplayName("Configuration 클래스는 CGLIB 프록시를 생성한다")
    void instantiate_configurationProxy() throws Exception {
        // given
        Constructor<SimpleService> constructor = SimpleService.class.getDeclaredConstructor();
        BeanDefinition def = createMockBeanDefinition(
                "config",
                SimpleService.class,
                constructor,
                new Class<?>[0],
                true  // Configuration proxy needed
        );

        when(mockResolver.resolve(any(), any(), any())).thenReturn(new Object[0]);

        // when
        Object result = strategy.instantiate(def, mockResolver, mockBeanFactory);

        // then
        assertNotNull(result);
        // CGLIB 프록시는 원본 클래스를 상속받으므로 instanceof가 true
        assertInstanceOf(SimpleService.class, result);
        // 프록시 객체인지 확인 (클래스 이름에 "$$" 포함)
        assertTrue(result.getClass().getName().contains("$$"));
    }

    @Test
    @DisplayName("생성자 호출 실패 시 예외를 발생시킨다")
    void instantiate_constructorThrowsException() throws Exception {
        // given
        Constructor<ServiceWithDependency> constructor =
            ServiceWithDependency.class.getDeclaredConstructor(SimpleService.class);

        BeanDefinition def = createMockBeanDefinition(
                "service",
                ServiceWithDependency.class,
                constructor,
                new Class<?>[]{SimpleService.class},
                false
        );

        // null을 전달하여 생성자에서 NullPointerException 발생 (SimpleService가 null이어도 객체는 생성됨)
        // 대신 resolver가 예외를 던지도록 설정
        when(mockResolver.resolve(any(), any(), any())).thenThrow(new RuntimeException("Dependency resolution failed"));

        // when & then
        assertThrows(RuntimeException.class, () -> strategy.instantiate(def, mockResolver, mockBeanFactory));
    }

    // Helper method
    private <T> BeanDefinition createMockBeanDefinition(
            String name,
            Class<T> type,
            Constructor<T> constructor,
            Class<?>[] argTypes,
            boolean isConfigProxy) {

        return new BeanDefinition() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Class<?> getType() {
                return type;
            }

            @Override
            public BeanCreationMethod getCreationMethod() {
                return BeanCreationMethod.CONSTRUCTOR;
            }

            @Override
            public Constructor<?> getConstructor() {
                return constructor;
            }

            @Override
            public Class<?>[] getConstructorArgumentTypes() {
                return argTypes;
            }

            @Override
            public java.lang.reflect.Method getFactoryMethod() {
                return null;
            }

            @Override
            public String getFactoryBeanName() {
                return null;
            }

            @Override
            public Class<?>[] getFactoryMethodArgumentTypes() {
                return new Class<?>[0];
            }

            @Override
            public boolean isProxyTarget() {
                return false;
            }

            @Override
            public boolean isConfigurationClassProxyNeeded() {
                return isConfigProxy;
            }

            @Override
            public boolean isPrimary() {
                return false;
            }
        };
    }
}
