package sprout.context.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanDefinition;
import sprout.context.BeanFactory;
import sprout.context.ContextInitializer;
import sprout.context.builtins.DefaultListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContextInitializerPhaseTest {

    private ContextInitializerPhase phase;
    private DefaultListableBeanFactory mockFactory;
    private List<BeanDefinition> infraDefs;
    private List<BeanDefinition> appDefs;
    private List<String> basePackages;

    static class TestContextInitializer implements ContextInitializer {
        boolean called = false;
        BeanFactory receivedFactory;

        @Override
        public void initializeAfterRefresh(BeanFactory beanFactory) {
            called = true;
            receivedFactory = beanFactory;
        }
    }

    @BeforeEach
    void setUp() {
        phase = new ContextInitializerPhase();
        mockFactory = mock(DefaultListableBeanFactory.class);
        infraDefs = new ArrayList<>();
        appDefs = new ArrayList<>();
        basePackages = List.of("com.example");
    }

    @Test
    @DisplayName("Phase 이름을 반환한다")
    void getName() {
        // when
        String name = phase.getName();

        // then
        assertEquals("ContextInitializer Execution", name);
    }

    @Test
    @DisplayName("order는 400이다")
    void getOrder() {
        // when
        int order = phase.getOrder();

        // then
        assertEquals(400, order);
    }

    @Test
    @DisplayName("모든 ContextInitializer를 실행한다")
    void execute_callsAllContextInitializers() throws Exception {
        // given
        TestContextInitializer initializer1 = new TestContextInitializer();
        TestContextInitializer initializer2 = new TestContextInitializer();
        TestContextInitializer initializer3 = new TestContextInitializer();

        when(mockFactory.getAllBeans(ContextInitializer.class))
                .thenReturn(List.of(initializer1, initializer2, initializer3));

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        assertTrue(initializer1.called);
        assertTrue(initializer2.called);
        assertTrue(initializer3.called);
        assertSame(mockFactory, initializer1.receivedFactory);
        assertSame(mockFactory, initializer2.receivedFactory);
        assertSame(mockFactory, initializer3.receivedFactory);
    }

    @Test
    @DisplayName("ContextInitializer가 없어도 정상 동작한다")
    void execute_withNoContextInitializers() throws Exception {
        // given
        when(mockFactory.getAllBeans(ContextInitializer.class)).thenReturn(List.of());

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when & then
        assertDoesNotThrow(() -> phase.execute(context));
    }

    @Test
    @DisplayName("ContextInitializer를 순서대로 실행한다")
    void execute_executesInOrder() throws Exception {
        // given
        List<String> executionOrder = new ArrayList<>();

        ContextInitializer initializer1 = bf -> executionOrder.add("init1");
        ContextInitializer initializer2 = bf -> executionOrder.add("init2");
        ContextInitializer initializer3 = bf -> executionOrder.add("init3");

        when(mockFactory.getAllBeans(ContextInitializer.class))
                .thenReturn(List.of(initializer1, initializer2, initializer3));

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        assertEquals(3, executionOrder.size());
        assertEquals("init1", executionOrder.get(0));
        assertEquals("init2", executionOrder.get(1));
        assertEquals("init3", executionOrder.get(2));
    }

    @Test
    @DisplayName("BeanFactory 인터페이스만으로도 동작한다")
    void execute_worksWithBeanFactoryInterface() throws Exception {
        // given
        BeanFactory genericFactory = mock(BeanFactory.class);
        when(genericFactory.getAllBeans(ContextInitializer.class)).thenReturn(List.of());

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                genericFactory, infraDefs, appDefs, basePackages
        );

        // when & then
        assertDoesNotThrow(() -> phase.execute(context));
    }

    @Test
    @DisplayName("단일 ContextInitializer를 실행한다")
    void execute_singleContextInitializer() throws Exception {
        // given
        TestContextInitializer initializer = new TestContextInitializer();
        when(mockFactory.getAllBeans(ContextInitializer.class)).thenReturn(List.of(initializer));

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        assertTrue(initializer.called);
        assertSame(mockFactory, initializer.receivedFactory);
    }

    @Test
    @DisplayName("ContextInitializer에서 예외가 발생하면 전파한다")
    void execute_propagatesExceptionFromInitializer() throws Exception {
        // given
        ContextInitializer failingInitializer = bf -> {
            throw new RuntimeException("Initializer failed");
        };

        when(mockFactory.getAllBeans(ContextInitializer.class))
                .thenReturn(List.of(failingInitializer));

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> phase.execute(context));

        assertEquals("Initializer failed", exception.getMessage());
    }

    @Test
    @DisplayName("PhaseContext의 다른 필드는 사용하지 않는다")
    void execute_doesNotUseOtherContextFields() throws Exception {
        // given
        when(mockFactory.getAllBeans(ContextInitializer.class)).thenReturn(List.of());

        // infraDefs, appDefs, basePackages를 null로 전달해도 동작해야 함
        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, null, null, null
        );

        // when & then
        assertDoesNotThrow(() -> phase.execute(context));
    }
}
