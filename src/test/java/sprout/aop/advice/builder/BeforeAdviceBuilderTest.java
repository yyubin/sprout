package sprout.aop.advice.builder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.aop.JoinPoint;
import sprout.aop.advice.Advice;
import sprout.aop.advice.interceptor.SimpleBeforeInterceptor;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.Pointcut;
import sprout.aop.advisor.PointcutFactory;
import sprout.aop.annotation.Before;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BeforeAdviceBuilderTest {

    BeforeAdviceBuilder builder = new BeforeAdviceBuilder();
    PointcutFactory pf = mock(PointcutFactory.class);
    Pointcut pc = mock(Pointcut.class);

    // Dummy annotation for @Before.annotation()
    @interface SomeAnno {}

    // ---------- sample aspect ----------
    static class AspectSample {
        @Before(annotation = SomeAnno.class, pointcut = "zero()")
        void zero() {}

        @Before(annotation = SomeAnno.class, pointcut = "jp()")
        void withJp(JoinPoint jp) {}

        @Before(annotation = SomeAnno.class, pointcut = "bad2()")
        void twoParams(JoinPoint jp, String s) {}

        @Before(annotation = SomeAnno.class, pointcut = "bad3()")
        void wrongSingleParam(String s) {}

        @Before(annotation = SomeAnno.class, pointcut = "staticP()")
        static void staticBefore(JoinPoint jp) {}
    }

    // ---------- helpers ----------
    private Method m(String name, Class<?>... params) {
        try {
            return AspectSample.class.getDeclaredMethod(name, params);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private Supplier<Object> sup() { return AspectSample::new; }

    // ---------- tests ----------

    @Test
    @DisplayName("0개 파라미터 허용")
    void zeroParam_ok() {
        when(pf.createPointcut(any(), anyString())).thenReturn(pc);

        Method method = m("zero");
        Advisor advisor = builder.build(AspectSample.class, method, sup(), pf);

        assertNotNull(advisor);
        assertEquals(pc, advisor.getPointcut());
        Advice advice = advisor.getAdvice();
        assertTrue(advice instanceof SimpleBeforeInterceptor);

        // createPointcut 인자 검증
        verify(pf).createPointcut(
                argThat(arr -> arr.length == 1 && arr[0] == SomeAnno.class),
                eq("zero()")
        );
    }

    @Test
    @DisplayName("JoinPoint 1개 파라미터 허용")
    void oneJoinPoint_ok() {
        when(pf.createPointcut(any(), anyString())).thenReturn(pc);

        Method method = m("withJp", JoinPoint.class);
        Advisor advisor = builder.build(AspectSample.class, method, sup(), pf);

        assertNotNull(advisor);
        assertTrue(advisor.getAdvice() instanceof SimpleBeforeInterceptor);
    }

    @Test
    @DisplayName("파라미터 2개면 IllegalStateException")
    void twoParams_illegal() {
        Method method = m("twoParams", JoinPoint.class, String.class);
        assertThrows(IllegalStateException.class,
                () -> builder.build(AspectSample.class, method, sup(), pf));
    }

    @Test
    @DisplayName("파라미터 1개지만 JoinPoint 아님 → IllegalStateException")
    void wrongParam_illegal() {
        Method method = m("wrongSingleParam", String.class);
        assertThrows(IllegalStateException.class,
                () -> builder.build(AspectSample.class, method, sup(), pf));
    }

    @Test
    @DisplayName("static 메서드면 Supplier 호출되지 않아야 한다")
    void staticMethod_supplierIgnored() {
        when(pf.createPointcut(any(), anyString())).thenReturn(pc);

        Supplier<Object> supplierMock = mock(Supplier.class);
        Method method = m("staticBefore", JoinPoint.class);

        Advisor advisor = builder.build(AspectSample.class, method, supplierMock, pf);

        assertNotNull(advisor);
        assertTrue(Modifier.isStatic(method.getModifiers()));
        assertTrue(advisor.getAdvice() instanceof SimpleBeforeInterceptor);
        verifyNoInteractions(supplierMock);
    }
}
