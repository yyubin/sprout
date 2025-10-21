package sprout.context.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanDefinition;
import sprout.beans.processor.BeanPostProcessor;
import sprout.context.BeanFactory;
import sprout.context.builtins.DefaultListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BeanPostProcessorRegistrationPhaseTest {

    private BeanPostProcessorRegistrationPhase phase;
    private DefaultListableBeanFactory mockFactory;
    private List<BeanDefinition> infraDefs;
    private List<BeanDefinition> appDefs;
    private List<String> basePackages;

    static class TestBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessBeforeInitialization(String beanName, Object bean) {
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(String beanName, Object bean) {
            return bean;
        }
    }

    @BeforeEach
    void setUp() {
        phase = new BeanPostProcessorRegistrationPhase();
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
        assertEquals("BeanPostProcessor Registration", name);
    }

    @Test
    @DisplayName("order는 200이다")
    void getOrder() {
        // when
        int order = phase.getOrder();

        // then
        assertEquals(200, order);
    }

    @Test
    @DisplayName("모든 BeanPostProcessor를 BeanFactory에 등록한다")
    void execute_registersAllBeanPostProcessors() throws Exception {
        // given
        BeanPostProcessor processor1 = new TestBeanPostProcessor();
        BeanPostProcessor processor2 = new TestBeanPostProcessor();
        BeanPostProcessor processor3 = new TestBeanPostProcessor();

        when(mockFactory.getAllBeans(BeanPostProcessor.class))
                .thenReturn(List.of(processor1, processor2, processor3));

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        verify(mockFactory).addBeanPostProcessor(processor1);
        verify(mockFactory).addBeanPostProcessor(processor2);
        verify(mockFactory).addBeanPostProcessor(processor3);
    }

    @Test
    @DisplayName("BeanPostProcessor가 없어도 정상 동작한다")
    void execute_withNoBeanPostProcessors() throws Exception {
        // given
        when(mockFactory.getAllBeans(BeanPostProcessor.class)).thenReturn(List.of());

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when & then
        assertDoesNotThrow(() -> phase.execute(context));
        verify(mockFactory, never()).addBeanPostProcessor(any());
    }

    @Test
    @DisplayName("BeanPostProcessor를 순서대로 등록한다")
    void execute_registersInOrder() throws Exception {
        // given
        BeanPostProcessor processor1 = mock(BeanPostProcessor.class, "processor1");
        BeanPostProcessor processor2 = mock(BeanPostProcessor.class, "processor2");
        BeanPostProcessor processor3 = mock(BeanPostProcessor.class, "processor3");

        when(mockFactory.getAllBeans(BeanPostProcessor.class))
                .thenReturn(List.of(processor1, processor2, processor3));

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        // InOrder를 사용하여 순서 검증
        var inOrder = inOrder(mockFactory);
        inOrder.verify(mockFactory).addBeanPostProcessor(processor1);
        inOrder.verify(mockFactory).addBeanPostProcessor(processor2);
        inOrder.verify(mockFactory).addBeanPostProcessor(processor3);
    }

    @Test
    @DisplayName("BeanFactory가 DefaultListableBeanFactory가 아니면 예외를 발생시킨다")
    void execute_throwsExceptionIfNotDefaultListableBeanFactory() {
        // given
        BeanFactory wrongFactory = mock(BeanFactory.class);
        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                wrongFactory, infraDefs, appDefs, basePackages
        );

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> phase.execute(context));

        assertTrue(exception.getMessage().contains("DefaultListableBeanFactory"));
    }

    @Test
    @DisplayName("단일 BeanPostProcessor를 등록한다")
    void execute_registersSingleBeanPostProcessor() throws Exception {
        // given
        BeanPostProcessor processor = new TestBeanPostProcessor();
        when(mockFactory.getAllBeans(BeanPostProcessor.class)).thenReturn(List.of(processor));

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        verify(mockFactory, times(1)).addBeanPostProcessor(processor);
    }

    @Test
    @DisplayName("PhaseContext의 다른 필드는 사용하지 않는다")
    void execute_doesNotUseOtherContextFields() throws Exception {
        // given
        when(mockFactory.getAllBeans(BeanPostProcessor.class)).thenReturn(List.of());

        // infraDefs, appDefs, basePackages를 null로 전달해도 동작해야 함
        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, null, null, null
        );

        // when & then
        assertDoesNotThrow(() -> phase.execute(context));
    }
}
