package sprout.beans.matching;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BeanTypeMatchingServiceTest {

    private BeanTypeMatchingService service;
    private Map<String, BeanDefinition> beanDefinitions;
    private Map<String, Object> singletons;

    interface Service {}
    interface Repository {}
    static class UserService implements Service {}
    static class OrderService implements Service {}
    static class UserRepository implements Repository {}

    @BeforeEach
    void setUp() {
        beanDefinitions = new HashMap<>();
        singletons = new HashMap<>();
        service = new BeanTypeMatchingService(beanDefinitions, singletons);
    }

    @Test
    @DisplayName("싱글톤에서 타입에 맞는 빈 이름을 찾는다")
    void findCandidateNamesForType_fromSingletons() {
        // given
        singletons.put("userService", new UserService());
        singletons.put("orderService", new OrderService());

        // when
        Set<String> candidates = service.findCandidateNamesForType(Service.class);

        // then
        assertEquals(2, candidates.size());
        assertTrue(candidates.contains("userService"));
        assertTrue(candidates.contains("orderService"));
    }

    @Test
    @DisplayName("BeanDefinition에서 타입에 맞는 빈 이름을 찾는다")
    void findCandidateNamesForType_fromBeanDefinitions() {
        // given
        beanDefinitions.put("userService", createMockBeanDefinition("userService", UserService.class));
        beanDefinitions.put("orderService", createMockBeanDefinition("orderService", OrderService.class));

        // when
        Set<String> candidates = service.findCandidateNamesForType(Service.class);

        // then
        assertEquals(2, candidates.size());
        assertTrue(candidates.contains("userService"));
        assertTrue(candidates.contains("orderService"));
    }

    @Test
    @DisplayName("싱글톤과 BeanDefinition을 모두 확인하여 후보를 찾는다")
    void findCandidateNamesForType_combinedSources() {
        // given
        singletons.put("userService", new UserService());
        beanDefinitions.put("orderService", createMockBeanDefinition("orderService", OrderService.class));

        // when
        Set<String> candidates = service.findCandidateNamesForType(Service.class);

        // then
        assertEquals(2, candidates.size());
        assertTrue(candidates.contains("userService"));
        assertTrue(candidates.contains("orderService"));
    }

    @Test
    @DisplayName("타입이 일치하지 않으면 빈 Set을 반환한다")
    void findCandidateNamesForType_noMatch() {
        // given
        singletons.put("userService", new UserService());

        // when
        Set<String> candidates = service.findCandidateNamesForType(Repository.class);

        // then
        assertTrue(candidates.isEmpty());
    }

    @Test
    @DisplayName("@Primary 어노테이션이 있는 빈을 선택한다")
    void choosePrimary_withPrimaryAnnotation() {
        // given
        BeanDefinition primaryDef = createMockBeanDefinition("userService", UserService.class, true);
        BeanDefinition normalDef = createMockBeanDefinition("orderService", OrderService.class, false);

        beanDefinitions.put("userService", primaryDef);
        beanDefinitions.put("orderService", normalDef);

        Set<String> candidates = Set.of("userService", "orderService");
        Map<Class<?>, String> primaryTypeToNameMap = new HashMap<>();

        // when
        String primary = service.choosePrimary(Service.class, candidates, primaryTypeToNameMap);

        // then
        assertEquals("userService", primary);
    }

    @Test
    @DisplayName("여러 개의 @Primary 빈이 있으면 예외를 발생시킨다")
    void choosePrimary_multiplePrimary_throwsException() {
        // given
        BeanDefinition primary1 = createMockBeanDefinition("userService", UserService.class, true);
        BeanDefinition primary2 = createMockBeanDefinition("orderService", OrderService.class, true);

        beanDefinitions.put("userService", primary1);
        beanDefinitions.put("orderService", primary2);

        Set<String> candidates = Set.of("userService", "orderService");
        Map<Class<?>, String> primaryTypeToNameMap = new HashMap<>();

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.choosePrimary(Service.class, candidates, primaryTypeToNameMap));

        assertTrue(exception.getMessage().contains("@Primary beans conflict"));
    }

    @Test
    @DisplayName("@Primary가 없으면 primaryTypeToNameMap에서 선택한다")
    void choosePrimary_fallbackToPrimaryMap() {
        // given
        beanDefinitions.put("userService", createMockBeanDefinition("userService", UserService.class, false));
        beanDefinitions.put("orderService", createMockBeanDefinition("orderService", OrderService.class, false));

        Set<String> candidates = Set.of("userService", "orderService");
        Map<Class<?>, String> primaryTypeToNameMap = new HashMap<>();
        primaryTypeToNameMap.put(Service.class, "userService");

        // when
        String primary = service.choosePrimary(Service.class, candidates, primaryTypeToNameMap);

        // then
        assertEquals("userService", primary);
    }

    @Test
    @DisplayName("@Primary도 없고 primaryMap에도 없으면 null을 반환한다")
    void choosePrimary_noPrimary_returnsNull() {
        // given
        beanDefinitions.put("userService", createMockBeanDefinition("userService", UserService.class, false));

        Set<String> candidates = Set.of("userService");
        Map<Class<?>, String> primaryTypeToNameMap = new HashMap<>();

        // when
        String primary = service.choosePrimary(Service.class, candidates, primaryTypeToNameMap);

        // then
        assertNull(primary);
    }

    @Test
    @DisplayName("getBeanNamesForType은 BeanDefinition만 확인한다")
    void getBeanNamesForType_onlyChecksDefinitions() {
        // given
        singletons.put("userService", new UserService());  // 이건 무시됨
        beanDefinitions.put("orderService", createMockBeanDefinition("orderService", OrderService.class));
        beanDefinitions.put("userRepo", createMockBeanDefinition("userRepo", UserRepository.class));

        // when
        Set<String> names = service.getBeanNamesForType(Service.class);

        // then
        assertEquals(1, names.size());
        assertTrue(names.contains("orderService"));
        assertFalse(names.contains("userService"));  // 싱글톤은 포함 안됨
    }

    // Helper methods
    private BeanDefinition createMockBeanDefinition(String name, Class<?> type) {
        return createMockBeanDefinition(name, type, false);
    }

    private BeanDefinition createMockBeanDefinition(String name, Class<?> type, boolean primary) {
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
                try {
                    return type.getDeclaredConstructor();
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }
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
                return primary;
            }
        };
    }
}
