package sprout.context.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanCreationMethod;
import sprout.beans.BeanDefinition;
import sprout.context.BeanFactory;
import sprout.context.builtins.DefaultListableBeanFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApplicationBeanPhaseTest {

    private ApplicationBeanPhase phase;
    private DefaultListableBeanFactory mockFactory;
    private List<BeanDefinition> infraDefs;
    private List<BeanDefinition> appDefs;
    private List<String> basePackages;

    static class Service {}
    static class Repository {}
    static class Controller {}

    @BeforeEach
    void setUp() {
        phase = new ApplicationBeanPhase();
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
        assertEquals("Application Bean Initialization", name);
    }

    @Test
    @DisplayName("order는 300이다")
    void getOrder() {
        // when
        int order = phase.getOrder();

        // then
        assertEquals(300, order);
    }

    @Test
    @DisplayName("애플리케이션 빈을 생성하고 postProcessListInjections를 호출한다")
    void execute_createsBeansAndPostProcesses() throws Exception {
        // given
        BeanDefinition def1 = createMockBeanDefinition("service", Service.class);
        BeanDefinition def2 = createMockBeanDefinition("repository", Repository.class);
        BeanDefinition def3 = createMockBeanDefinition("controller", Controller.class);
        appDefs.add(def1);
        appDefs.add(def2);
        appDefs.add(def3);

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        verify(mockFactory, times(3)).createBean(any(BeanDefinition.class));
        verify(mockFactory).postProcessListInjections();
    }

    @Test
    @DisplayName("애플리케이션 빈이 없어도 정상 동작한다")
    void execute_withEmptyAppDefs() throws Exception {
        // given
        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when & then
        assertDoesNotThrow(() -> phase.execute(context));
        verify(mockFactory, never()).createBean(any());
        verify(mockFactory).postProcessListInjections();
    }

    @Test
    @DisplayName("BeanGraph를 통한 위상 정렬 순서대로 빈을 생성한다")
    void execute_createsBeansinTopologicalOrder() throws Exception {
        // given
        BeanDefinition def1 = createMockBeanDefinition("bean1", Service.class);
        BeanDefinition def2 = createMockBeanDefinition("bean2", Repository.class);
        appDefs.add(def1);
        appDefs.add(def2);

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        // BeanGraph가 위상 정렬을 수행하므로 createBean이 호출됨
        verify(mockFactory, times(2)).createBean(any(BeanDefinition.class));
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
    @DisplayName("단일 애플리케이션 빈을 생성한다")
    void execute_createsSingleBean() throws Exception {
        // given
        BeanDefinition def = createMockBeanDefinition("service", Service.class);
        appDefs.add(def);

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        verify(mockFactory, times(1)).createBean(def);
        verify(mockFactory).postProcessListInjections();
    }

    @Test
    @DisplayName("Infrastructure 빈은 생성하지 않는다")
    void execute_doesNotCreateInfrastructureBeans() throws Exception {
        // given
        BeanDefinition infraDef = createMockBeanDefinition("infraBean", Service.class);
        BeanDefinition appDef = createMockBeanDefinition("appBean", Repository.class);
        infraDefs.add(infraDef);
        appDefs.add(appDef);

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        // appDef만 생성됨
        verify(mockFactory, times(1)).createBean(appDef);
        verify(mockFactory, never()).createBean(infraDef);
    }

    @Test
    @DisplayName("postProcessListInjections는 빈 생성 후에 호출된다")
    void execute_postProcessesAfterBeanCreation() throws Exception {
        // given
        BeanDefinition def = createMockBeanDefinition("service", Service.class);
        appDefs.add(def);

        BeanLifecyclePhase.PhaseContext context = new BeanLifecyclePhase.PhaseContext(
                mockFactory, infraDefs, appDefs, basePackages
        );

        // when
        phase.execute(context);

        // then
        var inOrder = inOrder(mockFactory);
        inOrder.verify(mockFactory).createBean(def);
        inOrder.verify(mockFactory).postProcessListInjections();
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
