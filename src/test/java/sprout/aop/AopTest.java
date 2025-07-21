package sprout.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import sprout.aop.advice.AdviceFactory;
import sprout.aop.advisor.AdvisorRegistry;
import sprout.aop.advisor.PointcutFactory;
import sprout.aop.annotation.Around;
import sprout.aop.annotation.Aspect;
import sprout.beans.processor.BeanPostProcessor;
import sprout.context.ApplicationContext;
import sprout.context.Container;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AopTest {

    private ApplicationContext container;
    private AspectPostProcessor aspectPostProcessor;
    private AdvisorRegistry advisorRegistry;
    private static List<String> executionLog = new ArrayList<>();

    @BeforeEach
    void setUp() {
        container.reset();
        advisorRegistry = new AdvisorRegistry();
        aspectPostProcessor = new AspectPostProcessor(advisorRegistry, container, new AdviceFactory(Mockito.mock(PointcutFactory.class)));
        container.registerRuntimeBean("aspectPostProcessor", aspectPostProcessor);

        List<String> basePackages = new ArrayList<>();
        basePackages.add("sprout.aop");
        aspectPostProcessor.initialize(basePackages);

        executionLog.clear();
    }

    @Aspect
    public static class TestAspect {
        @Around(pointcut = "execution(* sprout.aop.AopTest$TestService.doSomething(..))")
        public Object aroundAdvice(ProceedingJoinPoint pjp) throws Throwable {
            executionLog.add("Before advice");
            Object result = pjp.proceed();
            executionLog.add("After advice");
            return result;
        }
    }

    public static class TestService {
        public String doSomething() {
            executionLog.add("Executing doSomething");
            return "Hello from TestService";
        }
    }

    @Test
    void testAopProxy() {
        container.registerRuntimeBean("testAspect", new TestAspect());
        TestService originalService = new TestService();
        container.registerRuntimeBean("testService", originalService);

        TestService proxiedService = (TestService) aspectPostProcessor.postProcessAfterInitialization("testService", originalService);

        String result = proxiedService.doSomething();

        assertEquals("Hello from TestService", result);
        assertEquals(3, executionLog.size());
        assertEquals("Before advice", executionLog.get(0));
        assertEquals("Executing doSomething", executionLog.get(1));
        assertEquals("After advice", executionLog.get(2));
    }
}
