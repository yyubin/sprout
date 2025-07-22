package sprout.aop.advice.interceptor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import sprout.aop.JoinPoint;
import sprout.aop.MethodInvocation;
import sprout.aop.advice.Advice;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimpleAfterInterceptorTest {

    // ---- dummy target & invocation result ----
    static class Target {
        String hello(String name) { return "hi " + name; }
        String boom() { throw new IllegalStateException("boom"); }
    }

    // ---- aspects for 0-param / 1-param ----
    static class After0Aspect {
        boolean called;
        void after() { called = true; }
    }

    static class After1Aspect {
        JoinPoint captured;
        void after(JoinPoint jp) { captured = jp; }
    }

    // --- helper: build MethodInvocation mock quickly
    private MethodInvocation invMock(Object target, Method m, Object... args) {
        MethodInvocation mi = mock(MethodInvocation.class);
        when(mi.getMethod()).thenReturn(m);
        when(mi.getArguments()).thenReturn(args);
        when(mi.getArgs()).thenReturn(args);          // if your JoinPoint has this
        when(mi.getTarget()).thenReturn(target);      // if your JoinPoint has this
        return mi;
    }

    private static Method m(Class<?> c, String name, Class<?>... p) throws Exception {
        Method md = c.getDeclaredMethod(name, p);
        md.setAccessible(true);
        return md;
    }

    @Test
    @DisplayName("proceed 성공 후 0-파라미터 after 호출")
    void after0_success() throws Throwable {
        Target t = new Target();
        Method targetM = m(Target.class, "hello", String.class);
        targetM.setAccessible(true);
        MethodInvocation mi = invMock(t, targetM, "kim");
        when(mi.proceed()).thenReturn("hi kim");

        After0Aspect asp = new After0Aspect();
        Method afterM = m(After0Aspect.class, "after");

        Advice advice = new SimpleAfterInterceptor(() -> asp, afterM);

        Object ret = advice.invoke(mi);

        assertEquals("hi kim", ret);
        assertTrue(asp.called);
        verify(mi).proceed();
    }

    @Test
    @DisplayName("proceed 성공 후 JoinPoint 파라미터 전달")
    void after1_success_withJoinPoint() throws Throwable {
        Target t = new Target();
        Method targetM = m(Target.class, "hello", String.class);
        targetM.setAccessible(true);
        MethodInvocation mi = invMock(t, targetM, "lee");
        when(mi.proceed()).thenReturn("hi lee");

        After1Aspect asp = new After1Aspect();
        Method afterM = m(After1Aspect.class,"after", JoinPoint.class);
        afterM.setAccessible(true);

        Advice advice = new SimpleAfterInterceptor(() -> asp, afterM);

        Object ret = advice.invoke(mi);

        assertEquals("hi lee", ret);
        assertNotNull(asp.captured);
        assertEquals(targetM, asp.captured.getMethod());
        assertArrayEquals(new Object[]{"lee"}, asp.captured.getArgs());
        assertSame(t, asp.captured.getTarget());
    }

    @Test
    @DisplayName("타깃에서 예외가 나도 after는 실행되고, 예외는 다시 던져짐")
    void after_runs_even_when_target_throws() throws Throwable {
        Target t = new Target();
        Method boom    = m(Target.class, "boom");

        MethodInvocation mi = invMock(t, boom);
        when(mi.proceed()).thenThrow(new IllegalStateException("boom"));

        After0Aspect asp = new After0Aspect();
        Method afterM = m(After0Aspect.class, "after");
        afterM.setAccessible(true);
        Advice advice = new SimpleAfterInterceptor(() -> asp, afterM);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> advice.invoke(mi));
        assertEquals("boom", ex.getMessage());
        assertTrue(asp.called); // after는 실행됨
    }

    @Test
    @DisplayName("after 메서드가 예외를 던지면 그 예외를 그대로 던짐")
    void after_throws_propagated() throws Throwable {
        Target t = new Target();
        Method targetM = m(Target.class, "hello", String.class);
        MethodInvocation mi = invMock(t, targetM, "x");
        when(mi.proceed()).thenReturn("hi x");

        class BadAspect {
            void after() { throw new RuntimeException("after err"); }
        }
        BadAspect asp = new BadAspect();
        Method afterM = m(BadAspect.class, "after");

        Advice advice = new SimpleAfterInterceptor(() -> asp, afterM);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> advice.invoke(mi));
        assertEquals("after err", ex.getMessage());
    }

    @Test
    @DisplayName("static after 메서드면 Supplier는 호출되지 않아도 됨")
    void static_after_supplier_not_used() throws Throwable {
        class StaticAspect {
            static void after() {}
        }
        Method afterM = m(StaticAspect.class,"after");

        Supplier<Object> sup = mock(Supplier.class); // 호출 안되기를 기대

        Target t = new Target();
        Method m = m(Target.class, "hello", String.class);
        MethodInvocation mi = invMock(t, m, "yo");
        when(mi.proceed()).thenReturn("hi yo");

        Advice advice = new SimpleAfterInterceptor(sup, afterM);
        Object out = advice.invoke(mi);
        assertEquals("hi yo", out);

        verifyNoInteractions(sup);
    }
}
