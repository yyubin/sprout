package sprout.beans.instantiation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.context.BeanFactory;

import java.lang.reflect.Parameter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SingleBeanDependencyResolverTest {

    private SingleBeanDependencyResolver resolver;
    private BeanFactory mockBeanFactory;

    static class Service {}
    static class Repository {}

    @BeforeEach
    void setUp() {
        mockBeanFactory = mock(BeanFactory.class);
        resolver = new SingleBeanDependencyResolver(mockBeanFactory);
    }

    @Test
    @DisplayName("List가 아닌 모든 타입을 지원한다")
    void supports_nonListTypes() {
        // when & then
        assertTrue(resolver.supports(Service.class));
        assertTrue(resolver.supports(Repository.class));
        assertTrue(resolver.supports(String.class));
        assertTrue(resolver.supports(Integer.class));
    }

    @Test
    @DisplayName("List 타입은 지원하지 않는다")
    void supports_listType() {
        // when & then
        assertFalse(resolver.supports(List.class));
    }

    @Test
    @DisplayName("BeanFactory에서 타입으로 빈을 조회한다")
    void resolve_fetchesFromBeanFactory() {
        // given
        Class<?> type = Service.class;
        Parameter mockParam = mock(Parameter.class);
        BeanDefinition mockDef = createMockBeanDefinition("target", Service.class);

        Service expectedService = new Service();
        when(mockBeanFactory.getBean(Service.class)).thenReturn(expectedService);

        // when
        Object result = resolver.resolve(type, mockParam, mockDef);

        // then
        assertSame(expectedService, result);
        verify(mockBeanFactory).getBean(Service.class);
    }

    @Test
    @DisplayName("BeanFactory에서 빈을 찾을 수 없으면 예외를 전파한다")
    void resolve_beanNotFound_propagatesException() {
        // given
        Class<?> type = Service.class;
        Parameter mockParam = mock(Parameter.class);
        BeanDefinition mockDef = createMockBeanDefinition("target", Service.class);

        when(mockBeanFactory.getBean(Service.class))
                .thenThrow(new RuntimeException("No bean of type Service found"));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> resolver.resolve(type, mockParam, mockDef));

        assertTrue(exception.getMessage().contains("No bean of type Service found"));
    }

    @Test
    @DisplayName("다양한 타입의 빈을 해결할 수 있다")
    void resolve_variousTypes() {
        // given
        Service service = new Service();
        Repository repository = new Repository();
        String stringBean = "test";

        when(mockBeanFactory.getBean(Service.class)).thenReturn(service);
        when(mockBeanFactory.getBean(Repository.class)).thenReturn(repository);
        when(mockBeanFactory.getBean(String.class)).thenReturn(stringBean);

        Parameter mockParam = mock(Parameter.class);
        BeanDefinition mockDef = createMockBeanDefinition("target", Service.class);

        // when & then
        assertSame(service, resolver.resolve(Service.class, mockParam, mockDef));
        assertSame(repository, resolver.resolve(Repository.class, mockParam, mockDef));
        assertSame(stringBean, resolver.resolve(String.class, mockParam, mockDef));
    }

    // Helper method
    private BeanDefinition createMockBeanDefinition(String name, Class<?> type) {
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
            public java.lang.reflect.Constructor<?> getConstructor() {
                return null;
            }

            @Override
            public Class<?>[] getConstructorArgumentTypes() {
                return new Class<?>[0];
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
                return false;
            }

            @Override
            public boolean isPrimary() {
                return false;
            }
        };
    }
}
