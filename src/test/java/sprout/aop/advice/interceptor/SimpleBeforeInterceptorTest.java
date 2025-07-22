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

class SimpleBeforeInterceptorTest {

    // ---- 더미 타깃 ----
    static class Target {
        String hi(String name) { return "hi " + name; }
        String boom() { throw new IllegalStateException("boom"); }
    }

    // ---- 어드바이스용 Aspect ----
    static class Before0Aspect {
        boolean called;
        void before() { called = true; }
    }

    static class Before1Aspect {
        JoinPoint captured;
        void before(JoinPoint jp) { captured = jp; }
    }

    // ---- 리플렉션 헬퍼 ----
    private static Method m(Class<?> c, String name, Class<?>... p) throws Exception {
        Method md = c.getDeclaredMethod(name, p);
        md.setAccessible(true);
        return md;
    }

    // ---- MethodInvocation mock 헬퍼 ----
    private MethodInvocation invMock(Object target, Method m, Object... args) {
        MethodInvocation mi = mock(MethodInvocation.class);
        when(mi.getMethod()).thenReturn(m);
        when(mi.getArguments()).thenReturn(args);
        // JoinPointAdapter가 사용하는 메서드들 스텁
        when(mi.getArgs()).thenReturn(args);
        when(mi.getTarget()).thenReturn(target);
        return mi;
    }

    @Test
    @DisplayName("0-파라미터 before: 먼저 실행되고 proceed 결과 반환")
    void before0_success() throws Throwable {
        Target t = new Target();
        Method targetM = m(Target.class, "hi", String.class);

        MethodInvocation mi = invMock(t, targetM, "kim");
        when(mi.proceed()).thenReturn("hi kim");

        Before0Aspect asp = new Before0Aspect();
        Method beforeM = m(Before0Aspect.class, "before");

        Advice advice = new SimpleBeforeInterceptor(() -> asp, beforeM);

        Object out = advice.invoke(mi);

        assertEquals("hi kim", out);
        assertTrue(asp.called);

        InOrder in = inOrder(mi);
        // proceed가 호출되었는지만 체크 (정확한 순서까지 보려면 스파이/인디케이터 필요)
        in.verify(mi).proceed();
    }

    @Test
    @DisplayName("JoinPoint 파라미터 전달 확인")
    void before1_withJoinPoint() throws Throwable {
        Target t = new Target();
        Method targetM = m(Target.class, "hi", String.class);

        MethodInvocation mi = invMock(t, targetM, "lee");
        when(mi.proceed()).thenReturn("hi lee");

        Before1Aspect asp = new Before1Aspect();
        Method beforeM = m(Before1Aspect.class, "before", JoinPoint.class);

        Advice advice = new SimpleBeforeInterceptor(() -> asp, beforeM);

        Object out = advice.invoke(mi);

        assertEquals("hi lee", out);
        assertNotNull(asp.captured);
        assertEquals(targetM, asp.captured.getMethod());
        assertArrayEquals(new Object[]{"lee"}, asp.captured.getArgs());
        assertSame(t, asp.captured.getTarget());
    }

    @Test
    @DisplayName("타깃이 예외를 던지면 그대로 전파, before는 이미 실행됨")
    void targetThrows_propagated() throws Throwable {
        Target t = new Target();
        Method boom = m(Target.class, "boom");

        MethodInvocation mi = invMock(t, boom);
        when(mi.proceed()).thenThrow(new IllegalStateException("boom"));

        Before0Aspect asp = new Before0Aspect();
        Method beforeM = m(Before0Aspect.class, "before");

        Advice advice = new SimpleBeforeInterceptor(() -> asp, beforeM);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> advice.invoke(mi));
        assertEquals("boom", ex.getMessage());
        assertTrue(asp.called); // before는 실행됨
    }

    @Test
    @DisplayName("before 메서드가 예외 던지면 proceed 호출 전 예외 전파")
    void beforeThrows_propagated() throws Throwable {
        Target t = new Target();
        Method targetM = m(Target.class, "hi", String.class);
        MethodInvocation mi = invMock(t, targetM, "x");

        class BadAspect {
            void before() { throw new RuntimeException("before err"); }
        }
        BadAspect asp = new BadAspect();
        Method beforeM = m(BadAspect.class, "before");

        Advice advice = new SimpleBeforeInterceptor(() -> asp, beforeM);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> advice.invoke(mi));
        assertEquals("before err", ex.getMessage());

        verify(mi, never()).proceed(); // proceed 호출 안 됨
    }

    @Test
    @DisplayName("static before 메서드면 supplier는 호출 안 됨")
    void staticBefore_supplierIgnored() throws Throwable {
        class StaticAspect {
            static void before() {}
        }
        Method beforeM = m(StaticAspect.class, "before");

        Supplier<Object> sup = mock(Supplier.class); // 호출 안 되길 기대

        Target t = new Target();
        Method targetM = m(Target.class, "hi", String.class);
        MethodInvocation mi = invMock(t, targetM, "yo");
        when(mi.proceed()).thenReturn("hi yo");

        Advice advice = new SimpleBeforeInterceptor(sup, beforeM);
        Object out = advice.invoke(mi);
        assertEquals("hi yo", out);

        verifyNoInteractions(sup);
    }
}
