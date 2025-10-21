package sprout.beans.instantiation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CompositeDependencyResolverTest {

    private CompositeDependencyResolver resolver;
    private DependencyTypeResolver mockResolver1;
    private DependencyTypeResolver mockResolver2;

    static class Service {}
    static class Repository {}

    @BeforeEach
    void setUp() {
        mockResolver1 = mock(DependencyTypeResolver.class);
        mockResolver2 = mock(DependencyTypeResolver.class);

        List<DependencyTypeResolver> resolvers = new ArrayList<>();
        resolvers.add(mockResolver1);
        resolvers.add(mockResolver2);

        resolver = new CompositeDependencyResolver(resolvers);
    }

    @Test
    @DisplayName("첫 번째 resolver가 지원하면 해당 resolver를 사용한다")
    void resolve_usesFirstSupportingResolver() {
        // given
        Class<?>[] types = {Service.class};
        Parameter mockParam = mock(Parameter.class);
        Parameter[] params = {mockParam};
        BeanDefinition targetDef = createMockBeanDefinition("target", Service.class);

        Service expectedService = new Service();

        when(mockResolver1.supports(Service.class)).thenReturn(true);
        when(mockResolver1.resolve(eq(Service.class), eq(mockParam), eq(targetDef))).thenReturn(expectedService);

        // when
        Object[] result = resolver.resolve(types, params, targetDef);

        // then
        assertEquals(1, result.length);
        assertSame(expectedService, result[0]);
        verify(mockResolver1).resolve(eq(Service.class), eq(mockParam), eq(targetDef));
        verify(mockResolver2, never()).supports(any());
    }

    @Test
    @DisplayName("첫 번째가 지원하지 않으면 다음 resolver를 시도한다")
    void resolve_triesNextResolverIfFirstDoesNotSupport() {
        // given
        Class<?>[] types = {Service.class};
        Parameter mockParam = mock(Parameter.class);
        Parameter[] params = {mockParam};
        BeanDefinition targetDef = createMockBeanDefinition("target", Service.class);

        Service expectedService = new Service();

        when(mockResolver1.supports(Service.class)).thenReturn(false);
        when(mockResolver2.supports(Service.class)).thenReturn(true);
        when(mockResolver2.resolve(eq(Service.class), eq(mockParam), eq(targetDef))).thenReturn(expectedService);

        // when
        Object[] result = resolver.resolve(types, params, targetDef);

        // then
        assertEquals(1, result.length);
        assertSame(expectedService, result[0]);
        verify(mockResolver1).supports(Service.class);
        verify(mockResolver2).supports(Service.class);
        verify(mockResolver2).resolve(eq(Service.class), eq(mockParam), eq(targetDef));
    }

    @Test
    @DisplayName("여러 타입의 의존성을 순서대로 해결한다")
    void resolve_multipleTypes() {
        // given
        Class<?>[] types = {Service.class, Repository.class};
        Parameter mockParam1 = mock(Parameter.class);
        Parameter mockParam2 = mock(Parameter.class);
        Parameter[] params = {mockParam1, mockParam2};
        BeanDefinition targetDef = createMockBeanDefinition("target", Service.class);

        Service expectedService = new Service();
        Repository expectedRepo = new Repository();

        when(mockResolver1.supports(Service.class)).thenReturn(true);
        when(mockResolver1.resolve(eq(Service.class), eq(mockParam1), eq(targetDef))).thenReturn(expectedService);
        when(mockResolver1.supports(Repository.class)).thenReturn(false);

        when(mockResolver2.supports(Repository.class)).thenReturn(true);
        when(mockResolver2.resolve(eq(Repository.class), eq(mockParam2), eq(targetDef))).thenReturn(expectedRepo);

        // when
        Object[] result = resolver.resolve(types, params, targetDef);

        // then
        assertEquals(2, result.length);
        assertSame(expectedService, result[0]);
        assertSame(expectedRepo, result[1]);
    }

    @Test
    @DisplayName("지원하는 resolver가 없으면 예외를 발생시킨다")
    void resolve_noSupportingResolver_throwsException() {
        // given
        Class<?>[] types = {Service.class};
        Parameter mockParam = mock(Parameter.class);
        Parameter[] params = {mockParam};
        BeanDefinition targetDef = createMockBeanDefinition("target", Service.class);

        when(mockResolver1.supports(Service.class)).thenReturn(false);
        when(mockResolver2.supports(Service.class)).thenReturn(false);

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> resolver.resolve(types, params, targetDef));

        assertTrue(exception.getMessage().contains("No DependencyTypeResolver found"));
        assertTrue(exception.getMessage().contains(Service.class.getName()));
    }

    @Test
    @DisplayName("빈 배열을 전달하면 빈 배열을 반환한다")
    void resolve_emptyArray() {
        // given
        Class<?>[] types = {};
        Parameter[] params = new Parameter[0];
        BeanDefinition targetDef = createMockBeanDefinition("target", Service.class);

        // when
        Object[] result = resolver.resolve(types, params, targetDef);

        // then
        assertEquals(0, result.length);
        verify(mockResolver1, never()).supports(any());
        verify(mockResolver2, never()).supports(any());
    }

    @Test
    @DisplayName("resolver 체인의 순서가 유지된다")
    void resolve_maintainsResolverOrder() {
        // given
        Class<?>[] types = {Service.class};
        Parameter mockParam = mock(Parameter.class);
        Parameter[] params = {mockParam};
        BeanDefinition targetDef = createMockBeanDefinition("target", Service.class);

        Service service1 = new Service();
        Service service2 = new Service();

        // 두 resolver 모두 지원하지만 첫 번째가 우선
        when(mockResolver1.supports(Service.class)).thenReturn(true);
        when(mockResolver1.resolve(eq(Service.class), eq(mockParam), eq(targetDef))).thenReturn(service1);

        when(mockResolver2.supports(Service.class)).thenReturn(true);
        when(mockResolver2.resolve(eq(Service.class), eq(mockParam), eq(targetDef))).thenReturn(service2);

        // when
        Object[] result = resolver.resolve(types, params, targetDef);

        // then
        assertSame(service1, result[0]); // 첫 번째 resolver의 결과 사용
        verify(mockResolver1).resolve(eq(Service.class), eq(mockParam), eq(targetDef));
        verify(mockResolver2, never()).resolve(any(), any(), any()); // 두 번째는 호출되지 않음
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
