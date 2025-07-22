package sprout.aop.advice.builder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.aop.JoinPoint; // just to show it's different from ProceedingJoinPoint
import sprout.aop.ProceedingJoinPoint;
import sprout.aop.advice.Advice;
import sprout.aop.advice.interceptor.SimpleAroundInterceptor;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.Pointcut;
import sprout.aop.advisor.PointcutFactory;
import sprout.aop.annotation.Around;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AroundAdviceBuilderTest {

    AroundAdviceBuilder builder = new AroundAdviceBuilder();
    PointcutFactory pf = mock(PointcutFactory.class);
    Pointcut pc = mock(Pointcut.class);

    // ---- dummy annotation used in @Around ----
    @interface SomeAnno {}

    // ---- sample aspect ----
    static class AspectSample {

        @Around(annotation = SomeAnno.class, pointcut = "zero()")
        void zero() {}

        @Around(annotation = SomeAnno.class, pointcut = "pjp()")
        Object withPjp(ProceedingJoinPoint pjp) throws Throwable { return pjp.proceed(); }

        @Around(annotation = SomeAnno.class, pointcut = "bad2()")
        void twoParams(ProceedingJoinPoint pjp, String x) {}

        @Around(annotation = SomeAnno.class, pointcut = "bad3()")
        void wrongSingleParam(String x) {}

        @Around(annotation = SomeAnno.class, pointcut = "staticP()")
        static Object staticAround(ProceedingJoinPoint pjp) throws Throwable {
            return pjp.proceed();
        }
    }

    // ---- helpers ----
    private Method m(String name, Class<?>... params) throws NoSuchMethodException {
        return AspectSample.class.getDeclaredMethod(name, params);
    }

    private Supplier<Object> sup() { return AspectSample::new; }

    // ---- tests ----
    @Test
    @DisplayName("0개 파라미터면 IllegalStateException")
    void zeroParam_illegal() throws Exception {
        Method method = m("zero");
        assertThrows(IllegalStateException.class,
                () -> builder.build(AspectSample.class, method, sup(), pf));
    }

    @Test
    @DisplayName("ProceedingJoinPoint 한 개 파라미터 허용")
    void onePjp_ok() throws Exception {
        when(pf.createPointcut(any(), anyString())).thenReturn(pc);

        Method method = m("withPjp", ProceedingJoinPoint.class);
        Advisor advisor = builder.build(AspectSample.class, method, sup(), pf);

        assertNotNull(advisor);
        assertTrue(advisor.getAdvice() instanceof SimpleAroundInterceptor);
    }

    @Test
    @DisplayName("파라미터 2개면 IllegalStateException")
    void twoParams_illegal() throws Exception {
        Method method = m("twoParams", ProceedingJoinPoint.class, String.class);
        assertThrows(IllegalStateException.class,
                () -> builder.build(AspectSample.class, method, sup(), pf));
    }

    @Test
    @DisplayName("파라미터 1개지만 ProceedingJoinPoint 아님 → IllegalStateException")
    void wrongParam_illegal() throws Exception {
        Method method = m("wrongSingleParam", String.class);
        assertThrows(IllegalStateException.class,
                () -> builder.build(AspectSample.class, method, sup(), pf));
    }

    @Test
    @DisplayName("static 메서드면 Supplier는 사용되지 않아야 한다")
    void staticMethod_supplierIgnored() throws Exception {
        when(pf.createPointcut(any(), anyString())).thenReturn(pc);

        Supplier<Object> original = mock(Supplier.class);
        Method method = m("staticAround", ProceedingJoinPoint.class);

        Advisor advisor = builder.build(AspectSample.class, method, original, pf);
        assertNotNull(advisor);
        assertTrue(Modifier.isStatic(method.getModifiers()));
        assertTrue(advisor.getAdvice() instanceof SimpleAroundInterceptor);

        verifyNoInteractions(original); // supplier 호출 안 됨
    }
}
