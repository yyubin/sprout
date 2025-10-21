package sprout.beans.instantiation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.context.PendingListInjection;

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListBeanDependencyResolverTest {

    private ListBeanDependencyResolver resolver;
    private List<PendingListInjection> pendingListInjections;

    interface Service {}
    static class ServiceImpl implements Service {}

    @BeforeEach
    void setUp() {
        pendingListInjections = new ArrayList<>();
        resolver = new ListBeanDependencyResolver(pendingListInjections);
    }

    @Test
    @DisplayName("List 타입을 지원한다")
    void supports_listType() {
        // when & then
        assertTrue(resolver.supports(List.class));
        assertTrue(resolver.supports(ArrayList.class));
    }

    @Test
    @DisplayName("List가 아닌 타입은 지원하지 않는다")
    void supports_nonListType() {
        // when & then
        assertFalse(resolver.supports(Service.class));
        assertFalse(resolver.supports(String.class));
        assertFalse(resolver.supports(Object.class));
    }

    @Test
    @DisplayName("빈 List를 생성하고 pending 목록에 등록한다")
    void resolve_createsEmptyListAndRegistersPending() throws Exception {
        // given
        Parameter mockParam = createMockParameter(Service.class);
        BeanDefinition mockDef = createMockBeanDefinition("target", Service.class);

        // when
        Object result = resolver.resolve(List.class, mockParam, mockDef);

        // then
        assertNotNull(result);
        assertInstanceOf(List.class, result);
        List<?> resultList = (List<?>) result;
        assertTrue(resultList.isEmpty());

        // pending 목록에 등록되었는지 확인
        assertEquals(1, pendingListInjections.size());
        PendingListInjection pending = pendingListInjections.get(0);
        assertSame(resultList, pending.getListToPopulate());
        assertEquals(Service.class, pending.getGenericType());
    }

    @Test
    @DisplayName("제네릭 타입을 올바르게 추출한다")
    void resolve_extractsGenericType() throws Exception {
        // given
        Parameter mockParam = createMockParameter(ServiceImpl.class);
        BeanDefinition mockDef = createMockBeanDefinition("target", Service.class);

        // when
        resolver.resolve(List.class, mockParam, mockDef);

        // then
        assertEquals(1, pendingListInjections.size());
        PendingListInjection pending = pendingListInjections.get(0);
        assertEquals(ServiceImpl.class, pending.getGenericType());
    }

    @Test
    @DisplayName("여러 번 호출하면 각각 다른 List와 PendingListInjection을 생성한다")
    void resolve_multipleCallsCreateSeparateInstances() throws Exception {
        // given
        Parameter mockParam1 = createMockParameter(Service.class);
        Parameter mockParam2 = createMockParameter(ServiceImpl.class);
        BeanDefinition mockDef = createMockBeanDefinition("target", Service.class);

        // when
        Object result1 = resolver.resolve(List.class, mockParam1, mockDef);
        Object result2 = resolver.resolve(List.class, mockParam2, mockDef);

        // then
        assertNotSame(result1, result2);
        assertEquals(2, pendingListInjections.size());

        PendingListInjection pending1 = pendingListInjections.get(0);
        PendingListInjection pending2 = pendingListInjections.get(1);

        assertSame(result1, pending1.getListToPopulate());
        assertSame(result2, pending2.getListToPopulate());
        assertEquals(Service.class, pending1.getGenericType());
        assertEquals(ServiceImpl.class, pending2.getGenericType());
    }

    @Test
    @DisplayName("생성된 List는 mutable하다")
    void resolve_createdListIsMutable() throws Exception {
        // given
        Parameter mockParam = createMockParameter(Service.class);
        BeanDefinition mockDef = createMockBeanDefinition("target", Service.class);

        // when
        Object result = resolver.resolve(List.class, mockParam, mockDef);

        // then
        List<Object> resultList = (List<Object>) result;
        resultList.add(new ServiceImpl());
        assertEquals(1, resultList.size());
    }

    // Helper methods
    private Parameter createMockParameter(Class<?> genericType) {
        Parameter mockParam = mock(Parameter.class);

        // ParameterizedType 모킹
        ParameterizedType mockParameterizedType = mock(ParameterizedType.class);
        Type[] typeArgs = {genericType};

        when(mockParam.getParameterizedType()).thenReturn(mockParameterizedType);
        when(mockParameterizedType.getActualTypeArguments()).thenReturn(typeArgs);

        return mockParam;
    }

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
