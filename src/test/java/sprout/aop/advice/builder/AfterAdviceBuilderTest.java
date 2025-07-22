package sprout.aop.advice.builder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sprout.aop.JoinPoint;
import sprout.aop.advice.Advice;
import sprout.aop.advice.interceptor.SimpleAfterInterceptor;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.Pointcut;
import sprout.aop.advisor.PointcutFactory;
import sprout.aop.annotation.After;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AfterAdviceBuilderTest {

    AfterAdviceBuilder builder = new AfterAdviceBuilder();
    PointcutFactory pf = mock(PointcutFactory.class);
    Pointcut pc = mock(Pointcut.class);

    // ---------- fixture aspect ----------
    static class AspectSample {
        @After(annotation = SomeAnno.class, pointcut = "myPoint()")
        void zeroParam() {}

        @After(annotation = SomeAnno.class, pointcut = "p()")
        void withJoinPoint(JoinPoint jp) {}

        @After(annotation = SomeAnno.class, pointcut = "bad()")
        void twoParams(JoinPoint jp, String x) {}

        @After(annotation = SomeAnno.class, pointcut = "bad2()")
        void wrongSingleParam(String x) {}

        @After(annotation = SomeAnno.class, pointcut = "staticP()")
        static void staticAfter() {}
    }

    // dummy annotation used in @After
    @interface SomeAnno {}

    private Method m(String name, Class<?>... types) throws NoSuchMethodException {
        return AspectSample.class.getDeclaredMethod(name, types);
    }

    private Supplier<Object> sup() { return AspectSample::new; }

    // ---------- tests ----------

    @Test
    @DisplayName("0개 파라미터 메서드 빌드 성공")
    void zeroParam_ok() throws Exception {
        when(pf.createPointcut(any(), anyString())).thenReturn(pc);

        Method method = m("zeroParam");
        Advisor advisor = builder.build(AspectSample.class, method, sup(), pf);

        assertNotNull(advisor);
        assertEquals(pc, advisor.getPointcut());

        Advice advice = advisor.getAdvice();
        assertTrue(advice instanceof SimpleAfterInterceptor);
        // PointcutFactory 호출 검증
        ArgumentCaptor<String> exprCap = ArgumentCaptor.forClass(String.class);
        verify(pf).createPointcut(
                argThat(arr -> arr.length == 1 && arr[0] == SomeAnno.class),
                eq("myPoint()")
        );
    }

    @Test
    @DisplayName("JoinPoint 하나 파라미터도 허용")
    void oneJoinPoint_ok() throws Exception {
        when(pf.createPointcut(any(), anyString())).thenReturn(pc);

        Method method = m("withJoinPoint", JoinPoint.class);
        Advisor adv = builder.build(AspectSample.class, method, sup(), pf);

        assertNotNull(adv);
        assertTrue(adv.getAdvice() instanceof SimpleAfterInterceptor);
    }

    @Test
    @DisplayName("파라미터 2개 → IllegalStateException")
    void twoParams_illegal() throws Exception {
        Method method = m("twoParams", JoinPoint.class, String.class);
        assertThrows(IllegalStateException.class,
                () -> builder.build(AspectSample.class, method, sup(), pf));
    }

    @Test
    @DisplayName("파라미터 1개지만 JoinPoint 아님 → IllegalStateException")
    void wrongParam_illegal() throws Exception {
        Method method = m("wrongSingleParam", String.class);
        assertThrows(IllegalStateException.class,
                () -> builder.build(AspectSample.class, method, sup(), pf));
    }

    @Test
    @DisplayName("static 메서드 → aspectSup 대신 null 사용 (Supplier 무의미)")
    void staticMethod_safeSupplierNull() throws Exception {
        when(pf.createPointcut(any(), anyString())).thenReturn(pc);

        Supplier<Object> original = mock(Supplier.class);
        Method method = m("staticAfter");

        Advisor advisor = builder.build(AspectSample.class, method, original, pf);
        SimpleAfterInterceptor interceptor = (SimpleAfterInterceptor) advisor.getAdvice();

        // SimpleAfterInterceptor 내부 safe supplier 얻을 방법이 없다면,
        // static 메서드는 인스턴스 필요 없으므로 supplier가 호출되지 않는지만 검증
        // (build 시점에 호출 안 함)
        verifyNoInteractions(original);

        // 추가적으로 static 확인
        assertTrue(Modifier.isStatic(method.getModifiers()));
        assertNotNull(interceptor);
    }
}
