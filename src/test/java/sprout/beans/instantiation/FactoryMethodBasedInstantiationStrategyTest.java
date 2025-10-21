package sprout.beans.instantiation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.context.BeanFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FactoryMethodBasedInstantiationStrategyTest {

    private FactoryMethodBasedInstantiationStrategy strategy;
    private DependencyResolver mockResolver;
    private BeanFactory mockBeanFactory;

    // 테스트용 클래스들
    static class SimpleService {
        private final String value;

        public SimpleService(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    static class ConfigClass {
        public SimpleService simpleService() {
            return new SimpleService("from-factory");
        }

        public SimpleService serviceWithDependency(String prefix) {
            return new SimpleService(prefix + "-service");
        }

        private SimpleService privateFactoryMethod() {
            return new SimpleService("private-factory");
        }
    }

    @BeforeEach
    void setUp() {
        strategy = new FactoryMethodBasedInstantiationStrategy();
        mockResolver = mock(DependencyResolver.class);
        mockBeanFactory = mock(BeanFactory.class);
    }

    @Test
    @DisplayName("FACTORY_METHOD 생성 방식을 지원한다")
    void supports_factoryMethod() {
        // when & then
        assertTrue(strategy.supports(BeanCreationMethod.FACTORY_METHOD));
    }

    @Test
    @DisplayName("CONSTRUCTOR 생성 방식을 지원하지 않는다")
    void supports_constructor() {
        // when & then
        assertFalse(strategy.supports(BeanCreationMethod.CONSTRUCTOR));
    }

    @Test
    @DisplayName("인자가 없는 팩토리 메서드로 빈을 생성한다")
    void instantiate_noArgFactoryMethod() throws Exception {
        // given
        ConfigClass factoryBean = new ConfigClass();
        Method factoryMethod = ConfigClass.class.getMethod("simpleService");

        BeanDefinition def = createMockBeanDefinition(
                "simpleService",
                SimpleService.class,
                "configClass",
                factoryMethod,
                new Class<?>[0]
        );

        when(mockBeanFactory.getBean("configClass")).thenReturn(factoryBean);
        when(mockResolver.resolve(any(), any(), any())).thenReturn(new Object[0]);

        // when
        Object result = strategy.instantiate(def, mockResolver, mockBeanFactory);

        // then
        assertNotNull(result);
        assertInstanceOf(SimpleService.class, result);
        SimpleService service = (SimpleService) result;
        assertEquals("from-factory", service.getValue());
    }

    @Test
    @DisplayName("의존성이 있는 팩토리 메서드로 빈을 생성한다")
    void instantiate_withDependencies() throws Exception {
        // given
        ConfigClass factoryBean = new ConfigClass();
        Method factoryMethod = ConfigClass.class.getMethod("serviceWithDependency", String.class);

        BeanDefinition def = createMockBeanDefinition(
                "serviceWithDep",
                SimpleService.class,
                "configClass",
                factoryMethod,
                new Class<?>[]{String.class}
        );

        when(mockBeanFactory.getBean("configClass")).thenReturn(factoryBean);
        when(mockResolver.resolve(
                eq(new Class<?>[]{String.class}),
                any(Parameter[].class),
                eq(def)
        )).thenReturn(new Object[]{"test"});

        // when
        Object result = strategy.instantiate(def, mockResolver, mockBeanFactory);

        // then
        assertNotNull(result);
        assertInstanceOf(SimpleService.class, result);
        SimpleService service = (SimpleService) result;
        assertEquals("test-service", service.getValue());
    }

    @Test
    @DisplayName("private 팩토리 메서드도 접근하여 빈을 생성한다")
    void instantiate_privateFactoryMethod() throws Exception {
        // given
        ConfigClass factoryBean = new ConfigClass();
        Method factoryMethod = ConfigClass.class.getDeclaredMethod("privateFactoryMethod");

        BeanDefinition def = createMockBeanDefinition(
                "privateService",
                SimpleService.class,
                "configClass",
                factoryMethod,
                new Class<?>[0]
        );

        when(mockBeanFactory.getBean("configClass")).thenReturn(factoryBean);
        when(mockResolver.resolve(any(), any(), any())).thenReturn(new Object[0]);

        // when
        Object result = strategy.instantiate(def, mockResolver, mockBeanFactory);

        // then
        assertNotNull(result);
        assertInstanceOf(SimpleService.class, result);
        SimpleService service = (SimpleService) result;
        assertEquals("private-factory", service.getValue());
    }

    @Test
    @DisplayName("팩토리 빈을 찾을 수 없으면 예외를 발생시킨다")
    void instantiate_factoryBeanNotFound() throws Exception {
        // given
        Method factoryMethod = ConfigClass.class.getMethod("simpleService");

        BeanDefinition def = createMockBeanDefinition(
                "service",
                SimpleService.class,
                "nonExistentFactory",
                factoryMethod,
                new Class<?>[0]
        );

        when(mockBeanFactory.getBean("nonExistentFactory"))
                .thenThrow(new RuntimeException("No bean named 'nonExistentFactory'"));

        // when & then
        assertThrows(Exception.class, () -> strategy.instantiate(def, mockResolver, mockBeanFactory));
    }

    @Test
    @DisplayName("팩토리 메서드 호출 실패 시 예외를 발생시킨다")
    void instantiate_factoryMethodThrowsException() throws Exception {
        // given
        ConfigClass factoryBean = new ConfigClass();
        Method factoryMethod = ConfigClass.class.getMethod("serviceWithDependency", String.class);

        BeanDefinition def = createMockBeanDefinition(
                "service",
                SimpleService.class,
                "configClass",
                factoryMethod,
                new Class<?>[]{String.class}
        );

        when(mockBeanFactory.getBean("configClass")).thenReturn(factoryBean);
        // resolver가 예외를 던지도록 설정
        when(mockResolver.resolve(any(), any(), any())).thenThrow(new RuntimeException("Dependency resolution failed"));

        // when & then
        assertThrows(RuntimeException.class, () -> strategy.instantiate(def, mockResolver, mockBeanFactory));
    }

    @Test
    @DisplayName("DependencyResolver에 올바른 파라미터를 전달한다")
    void instantiate_callsResolverWithCorrectParameters() throws Exception {
        // given
        ConfigClass factoryBean = new ConfigClass();
        Method factoryMethod = ConfigClass.class.getMethod("serviceWithDependency", String.class);

        BeanDefinition def = createMockBeanDefinition(
                "service",
                SimpleService.class,
                "configClass",
                factoryMethod,
                new Class<?>[]{String.class}
        );

        when(mockBeanFactory.getBean("configClass")).thenReturn(factoryBean);
        when(mockResolver.resolve(any(), any(), any())).thenReturn(new Object[]{"test"});

        // when
        strategy.instantiate(def, mockResolver, mockBeanFactory);

        // then
        verify(mockResolver).resolve(
                eq(new Class<?>[]{String.class}),
                eq(factoryMethod.getParameters()),
                eq(def)
        );
    }

    // Helper method
    private BeanDefinition createMockBeanDefinition(
            String name,
            Class<?> type,
            String factoryBeanName,
            Method factoryMethod,
            Class<?>[] argTypes) {

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
                return BeanCreationMethod.FACTORY_METHOD;
            }

            @Override
            public java.lang.reflect.Constructor<?> getConstructor() {
                return null;
            }

            @Override
            public Class<?>[] getConstructorArgumentTypes() {
                return new Class<?>[0];
            }

            @Override
            public Method getFactoryMethod() {
                return factoryMethod;
            }

            @Override
            public String getFactoryBeanName() {
                return factoryBeanName;
            }

            @Override
            public Class<?>[] getFactoryMethodArgumentTypes() {
                return argTypes;
            }

            @Override
            public boolean isProxyTarget() {
                return false;
            }

            @Override
            public boolean isConfigurationClassProxyNeeded() {
                return false;
            }

            @Override
            public boolean isPrimary() {
                return false;
            }
        };
    }
}
