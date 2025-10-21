package sprout.context.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanDefinition;
import sprout.context.BeanFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BeanLifecycleManagerTest {

    private BeanFactory mockBeanFactory;
    private List<BeanDefinition> infraDefs;
    private List<BeanDefinition> appDefs;
    private List<String> basePackages;

    @BeforeEach
    void setUp() {
        mockBeanFactory = mock(BeanFactory.class);
        infraDefs = new ArrayList<>();
        appDefs = new ArrayList<>();
        basePackages = List.of("com.example");
    }

    @Test
    @DisplayName("Phase를 순서대로 실행한다")
    void executePhases_executesInOrder() throws Exception {
        // given
        List<String> executionOrder = new ArrayList<>();

        BeanLifecyclePhase phase1 = createMockPhase("Phase1", 100, executionOrder);
        BeanLifecyclePhase phase2 = createMockPhase("Phase2", 200, executionOrder);
        BeanLifecyclePhase phase3 = createMockPhase("Phase3", 300, executionOrder);

        List<BeanLifecyclePhase> phases = List.of(phase3, phase1, phase2); // 순서 섞어서 전달
        BeanLifecycleManager manager = new BeanLifecycleManager(phases);

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockBeanFactory, infraDefs, appDefs, basePackages
        );

        // when
        manager.executePhases(context);

        // then
        assertEquals(3, executionOrder.size());
        assertEquals("Phase1", executionOrder.get(0));
        assertEquals("Phase2", executionOrder.get(1));
        assertEquals("Phase3", executionOrder.get(2));
    }

    @Test
    @DisplayName("모든 Phase가 실행된다")
    void executePhases_executesAllPhases() throws Exception {
        // given
        BeanLifecyclePhase mockPhase1 = mock(BeanLifecyclePhase.class);
        BeanLifecyclePhase mockPhase2 = mock(BeanLifecyclePhase.class);
        BeanLifecyclePhase mockPhase3 = mock(BeanLifecyclePhase.class);

        when(mockPhase1.getOrder()).thenReturn(100);
        when(mockPhase2.getOrder()).thenReturn(200);
        when(mockPhase3.getOrder()).thenReturn(300);
        when(mockPhase1.getName()).thenReturn("Phase1");
        when(mockPhase2.getName()).thenReturn("Phase2");
        when(mockPhase3.getName()).thenReturn("Phase3");

        List<BeanLifecyclePhase> phases = List.of(mockPhase1, mockPhase2, mockPhase3);
        BeanLifecycleManager manager = new BeanLifecycleManager(phases);

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockBeanFactory, infraDefs, appDefs, basePackages
        );

        // when
        manager.executePhases(context);

        // then
        verify(mockPhase1).execute(context);
        verify(mockPhase2).execute(context);
        verify(mockPhase3).execute(context);
    }

    @Test
    @DisplayName("Phase가 예외를 던지면 전파한다")
    void executePhases_propagatesException() throws Exception {
        // given
        BeanLifecyclePhase mockPhase = mock(BeanLifecyclePhase.class);
        when(mockPhase.getOrder()).thenReturn(100);
        when(mockPhase.getName()).thenReturn("FailingPhase");
        doThrow(new RuntimeException("Phase failed")).when(mockPhase).execute(any());

        List<BeanLifecyclePhase> phases = List.of(mockPhase);
        BeanLifecycleManager manager = new BeanLifecycleManager(phases);

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockBeanFactory, infraDefs, appDefs, basePackages
        );

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> manager.executePhases(context));

        assertEquals("Phase failed", exception.getMessage());
    }

    @Test
    @DisplayName("빈 Phase 리스트도 처리할 수 있다")
    void executePhases_emptyList() throws Exception {
        // given
        List<BeanLifecyclePhase> phases = List.of();
        BeanLifecycleManager manager = new BeanLifecycleManager(phases);

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockBeanFactory, infraDefs, appDefs, basePackages
        );

        // when & then (예외 없이 완료)
        assertDoesNotThrow(() -> manager.executePhases(context));
    }

    @Test
    @DisplayName("동일한 order를 가진 Phase들은 추가된 순서대로 실행된다")
    void executePhases_sameOrder_maintainsInsertionOrder() throws Exception {
        // given
        List<String> executionOrder = new ArrayList<>();

        BeanLifecyclePhase phase1 = createMockPhase("Phase1", 100, executionOrder);
        BeanLifecyclePhase phase2 = createMockPhase("Phase2", 100, executionOrder);
        BeanLifecyclePhase phase3 = createMockPhase("Phase3", 100, executionOrder);

        List<BeanLifecyclePhase> phases = List.of(phase1, phase2, phase3);
        BeanLifecycleManager manager = new BeanLifecycleManager(phases);

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockBeanFactory, infraDefs, appDefs, basePackages
        );

        // when
        manager.executePhases(context);

        // then
        assertEquals(3, executionOrder.size());
        // 동일한 order일 때는 안정 정렬이므로 삽입 순서 유지
        assertEquals("Phase1", executionOrder.get(0));
        assertEquals("Phase2", executionOrder.get(1));
        assertEquals("Phase3", executionOrder.get(2));
    }

    @Test
    @DisplayName("PhaseContext의 모든 필드가 Phase에 전달된다")
    void executePhases_passesCorrectContext() throws Exception {
        // given
        BeanLifecyclePhase mockPhase = mock(BeanLifecyclePhase.class);
        when(mockPhase.getOrder()).thenReturn(100);
        when(mockPhase.getName()).thenReturn("TestPhase");

        List<BeanLifecyclePhase> phases = List.of(mockPhase);
        BeanLifecycleManager manager = new BeanLifecycleManager(phases);

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockBeanFactory, infraDefs, appDefs, basePackages
        );

        // when
        manager.executePhases(context);

        // then
        verify(mockPhase).execute(argThat(ctx ->
                ctx.getBeanFactory() == mockBeanFactory &&
                ctx.getInfraDefs() == infraDefs &&
                ctx.getAppDefs() == appDefs &&
                ctx.getBasePackages() == basePackages
        ));
    }

    // Helper method
    private BeanLifecyclePhase createMockPhase(String name, int order, List<String> executionOrder) {
        return new BeanLifecyclePhase() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public int getOrder() {
                return order;
            }

            @Override
            public void execute(PhaseContext context) {
                executionOrder.add(name);
            }
        };
    }
}
