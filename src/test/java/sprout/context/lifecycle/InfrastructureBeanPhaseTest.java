package sprout.context.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.beans.InfrastructureBean;
import sprout.beans.processor.BeanPostProcessor;
import sprout.context.BeanFactory;
import sprout.context.PostInfrastructureInitializer;
import sprout.context.builtins.DefaultListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InfrastructureBeanPhaseTest {

    private InfrastructureBeanPhase phase;
    private DefaultListableBeanFactory mockFactory;
    private List<BeanDefinition> infraDefs;
    private List<BeanDefinition> appDefs;
    private List<String> basePackages;

    static class TestInfraBean implements InfrastructureBean {}
    static class TestBeanPostProcessor implements BeanPostProcessor, InfrastructureBean {
        @Override
        public Object postProcessBeforeInitialization(String beanName, Object bean) {
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(String beanName, Object bean) {
            return bean;
        }
    }

    static class TestPostInfraInitializer implements PostInfrastructureInitializer {
        boolean called = false;
        BeanFactory receivedFactory;
        List<String> receivedPackages;

        @Override
        public void afterInfrastructureSetup(BeanFactory beanFactory, List<String> basePackages) {
            called = true;
            receivedFactory = beanFactory;
            receivedPackages = basePackages;
        }
    }

    @BeforeEach
    void setUp() {
        phase = new InfrastructureBeanPhase();
        mockFactory = mock(DefaultListableBeanFactory.class);
        infraDefs = new ArrayList<>();
        appDefs = new ArrayList<>();
        basePackages = List.of("com.example", "com.test");
    }

    @Test
    @DisplayName("Phase 이름을 반환한다")
    void getName() {
        // when
        String name = phase.getName();

        // then
        assertEquals("Infrastructure Bean Initialization", name);
    }

    @Test
    @DisplayName("order는 100이다")
    void getOrder() {
        // when
        int order = phase.getOrder();

        // then
        assertEquals(100, order);
    }

    @Test
    @DisplayName("Infrastructure 빈을 생성하고 postProcessListInjections를 호출한다")
    void execute_createsBeansAndPostProcesses() throws Exception {
        // given
        BeanDefinition def1 = createMockBeanDefinition("infraBean1", TestInfraBean.class);
        BeanDefinition def2 = createMockBeanDefinition("infraBean2", TestBeanPostProcessor.class);
        infraDefs.add(def1);
        infraDefs.add(def2);

        when(mockFactory.getAllBeans(PostInfrastructureInitializer.class)).thenReturn(List.of());

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        verify(mockFactory, times(2)).createBean(any(BeanDefinition.class));
        verify(mockFactory).postProcessListInjections();
    }

    @Test
    @DisplayName("PostInfrastructureInitializer를 실행한다")
    void execute_callsPostInfrastructureInitializers() throws Exception {
        // given
        TestPostInfraInitializer initializer1 = new TestPostInfraInitializer();
        TestPostInfraInitializer initializer2 = new TestPostInfraInitializer();

        when(mockFactory.getAllBeans(PostInfrastructureInitializer.class))
                .thenReturn(List.of(initializer1, initializer2));

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        assertTrue(initializer1.called);
        assertTrue(initializer2.called);
        assertSame(mockFactory, initializer1.receivedFactory);
        assertSame(mockFactory, initializer2.receivedFactory);
        assertEquals(basePackages, initializer1.receivedPackages);
        assertEquals(basePackages, initializer2.receivedPackages);
    }

    @Test
    @DisplayName("basePackages를 PostInfrastructureInitializer에 전달한다")
    void execute_passesBasePackagesToInitializers() throws Exception {
        // given
        TestPostInfraInitializer initializer = new TestPostInfraInitializer();
        when(mockFactory.getAllBeans(PostInfrastructureInitializer.class))
                .thenReturn(List.of(initializer));

        List<String> expectedPackages = List.of("com.example", "com.test", "com.another");
        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, expectedPackages
        );

        // when
        phase.execute(context);

        // then
        assertEquals(expectedPackages, initializer.receivedPackages);
        assertEquals(3, initializer.receivedPackages.size());
    }

    @Test
    @DisplayName("Infrastructure 빈이 없어도 정상 동작한다")
    void execute_withEmptyInfraDefs() throws Exception {
        // given
        when(mockFactory.getAllBeans(PostInfrastructureInitializer.class)).thenReturn(List.of());

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when & then
        assertDoesNotThrow(() -> phase.execute(context));
        verify(mockFactory, never()).createBean(any());
        verify(mockFactory).postProcessListInjections();
    }

    @Test
    @DisplayName("PostInfrastructureInitializer가 없어도 정상 동작한다")
    void execute_withoutPostInfrastructureInitializers() throws Exception {
        // given
        BeanDefinition def = createMockBeanDefinition("infraBean", TestInfraBean.class);
        infraDefs.add(def);

        when(mockFactory.getAllBeans(PostInfrastructureInitializer.class)).thenReturn(List.of());

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when & then
        assertDoesNotThrow(() -> phase.execute(context));
        verify(mockFactory).createBean(def);
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
                return false;
            }
        };
    }
}
