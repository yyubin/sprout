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
    void container_shouldInstantiateAllBeans() throws NoSuchMethodException {
        container.bootstrap(List.of("app.test"));

        assertNotNull(container.get(SomeComponent.class),          "SomeComponent 미생성");
        assertNotNull(container.get(SomeServiceImpl.class),        "SomeServiceImpl 미생성");
        assertNotNull(container.get(ServiceWithDependency.class),  "ServiceWithDependency 미생성");
        assertNotNull(container.get(ComponentWithListDependency.class),
                "ComponentWithListDependency 미생성");
    }

    @Test
    void container_shouldPerformConstructorInjection() throws NoSuchMethodException {
        container.bootstrap(List.of("app.test"));

        ServiceWithDependency svc = container.get(ServiceWithDependency.class);
        assertEquals("Done", svc.callService());
    }

    @Test
    void container_shouldPerformInterfaceInjection() throws NoSuchMethodException {
        container.bootstrap(List.of("app.test"));

        SomeService someService = container.get(SomeService.class);
        assertInstanceOf(SomeServiceImpl.class, someService);
        assertEquals("Done", someService.doSomething());
    }

    @Test
    void container_shouldPerformListInjection() throws NoSuchMethodException {
        container.bootstrap(List.of("app.test"));

        ComponentWithListDependency comp = container.get(ComponentWithListDependency.class);
        assertEquals(1, comp.getServiceCount(), "SomeService 구현체 수 불일치");
        assertInstanceOf(SomeServiceImpl.class, comp.getServices().get(0));
    }

    @Test
    void container_shouldDetectCircularDependency() {
        assertThrows(BeanGraph.CircularDependencyException.class,
                () -> container.bootstrap(List.of("app.circular")),
                "순환 참조를 감지하지 못했습니다.");
    }
}
