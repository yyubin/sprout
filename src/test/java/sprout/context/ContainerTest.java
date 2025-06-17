package sprout.context;

import app.test.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sprout.beans.internal.BeanGraph;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContainerTest {

    private Container container;

    @BeforeEach
    void setUp() {
        container = Container.getInstance();
        container.reset();
    }

    @Test
    void container_shouldInstantiateAllBeans() {
        container.bootstrap(List.of("app.test")); // app.test 패키지 스캔

        // 각 컴포넌트들이 빈으로 잘 생성되었는지 확인
        assertNotNull(container.get(SomeComponent.class));
        assertNotNull(container.get(SomeServiceImpl.class));
        assertNotNull(container.get(ServiceWithDependency.class));
        assertNotNull(container.get(ComponentWithListDependency.class));
    }

    @Test
    void container_shouldPerformConstructorInjection() {
        container.bootstrap(List.of("app.test"));

        ServiceWithDependency serviceWithDep = container.get(ServiceWithDependency.class);
        assertNotNull(serviceWithDep);
        // 의존성이 제대로 주입되어 SomeService의 메서드를 호출할 수 있는지 확인
        assertEquals("Done", serviceWithDep.callService());
    }

    @Test
    void container_shouldPerformInterfaceInjection() {
        container.bootstrap(List.of("app.test"));

        // 인터페이스 타입으로 빈을 가져올 수 있는지 확인
        SomeService someService = container.get(SomeService.class);
        assertNotNull(someService);
        // 가져온 빈이 실제 구현체인지 확인 (SomeServiceImpl)
        assertTrue(someService instanceof SomeServiceImpl);
        assertEquals("Done", someService.doSomething());
    }

    @Test
    void container_shouldPerformListInjection() {
        container.bootstrap(List.of("app.test"));

        ComponentWithListDependency listDepComponent = container.get(ComponentWithListDependency.class);
        assertNotNull(listDepComponent);
        // SomeService 구현체(SomeServiceImpl)가 List에 제대로 주입되었는지 확인
        assertEquals(1, listDepComponent.getServiceCount()); // SomeServiceImpl만 구현했으므로 1개
        assertTrue(listDepComponent.getServices().get(0) instanceof SomeServiceImpl);
    }

    @Test
    void container_shouldDetectCircularDependency() {
        // 순환 참조가 있는 패키지를 스캔할 때 예외가 발생하는지 확인
        // 주의: CircularDependencyA와 B는 @Component가 붙어있으므로 ClassPathScanner에 의해 스캔됨
        // 이 테스트는 Container가 이 두 빈을 처리할 때 BeanGraph에서 예외가 발생하는지 확인

        assertThrows(BeanGraph.CircularDependencyException.class, () -> {
            container.bootstrap(List.of("app.circular")); // CircularDependencyA, B가 있는 패키지
        });
    }

}