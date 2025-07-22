package sprout.aop.advice.interceptor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.aop.MethodInvocation;
import sprout.aop.ProceedingJoinPoint;
import sprout.aop.advice.Advice;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimpleAroundInterceptorTest {

    // ---- dummy target ----
    static class Target {
        String hi(String name) { return "hi " + name; }
        String boom() { throw new IllegalStateException("boom"); }
    }

    // ---- aspect with valid/invalid around advices ----
    static class Aspect {
        Object wrap(ProceedingJoinPoint pjp) throws Throwable {
            Object ret = pjp.proceed();
            return "wrapped-" + ret;
        }
        Object justProceed(ProceedingJoinPoint pjp) throws Throwable {
            return pjp.proceed();
        }
        Object throwsInAdvice(ProceedingJoinPoint pjp) {
            throw new RuntimeException("advice err");
        }
        static Object staticAround(ProceedingJoinPoint pjp) throws Throwable {
            return pjp.proceed();
        }
    }

    private static Method m(Class<?> c, String name, Class<?>... p) throws Exception {
        Method md = c.getDeclaredMethod(name, p);
        md.setAccessible(true);
        return md;
    }

    // MethodInvocation mock helper
    private MethodInvocation invMock(Object target, Method m, Object... args) {
        MethodInvocation mi = mock(MethodInvocation.class);
        when(mi.getMethod()).thenReturn(m);
        when(mi.getArguments()).thenReturn(args);
        // 만약 JoinPoint 인터페이스에 getArgs(), getTarget() 등이 있다면:
        when(mi.getArgs()).thenReturn(args);
        when(mi.getTarget()).thenReturn(target);
        return mi;
    }

    @Test
    @DisplayName("정상 흐름: advice에서 proceed 호출하고 결과 래핑")
    void around_wrapsResult() throws Throwable {
        Target t = new Target();
        Method targetM = m(Target.class, "hi", String.class);
        MethodInvocation mi = invMock(t, targetM, "kim");
        when(mi.proceed()).thenReturn("hi kim");

        Aspect asp = new Aspect();
        Method adviceM = m(Aspect.class, "wrap", ProceedingJoinPoint.class);

        Advice advice = new SimpleAroundInterceptor(() -> asp, adviceM);

        Object out = advice.invoke(mi);
        assertEquals("wrapped-hi kim", out);
        verify(mi).proceed();
    }

    @Test
    @DisplayName("advice 내부 예외는 그대로 전파")
    void adviceThrows_propagated() throws Throwable {
        Target t = new Target();
        Method targetM = m(Target.class, "hi", String.class);
        MethodInvocation mi = invMock(t, targetM, "x");
        when(mi.proceed()).thenReturn("hi x");

        Aspect asp = new Aspect();
        Method adviceM = m(Aspect.class, "throwsInAdvice", ProceedingJoinPoint.class);

        Advice advice = new SimpleAroundInterceptor(() -> asp, adviceM);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> advice.invoke(mi));
        assertEquals("advice err", ex.getMessage());
    }

    @Test
    @DisplayName("타깃이 예외를 던지면 advice에서 받은 예외를 그대로 던질 수 있다")
    void targetThrows_propagated() throws Throwable {
        Target t = new Target();
        Method boom = m(Target.class, "boom");
        MethodInvocation mi = invMock(t, boom);
        when(mi.proceed()).thenThrow(new IllegalStateException("boom"));

        Aspect asp = new Aspect();
        Method adviceM = m(Aspect.class, "justProceed", ProceedingJoinPoint.class);

        Advice advice = new SimpleAroundInterceptor(() -> asp, adviceM);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> advice.invoke(mi));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    @DisplayName("static advice면 supplier는 호출 안 됨(분기 추가 시)")
    void staticAdvice_supplierIgnored_ifBranched() throws Throwable {
        Method adviceM = m(Aspect.class, "staticAround", ProceedingJoinPoint.class);

        Supplier<Object> sup = mock(Supplier.class); // 호출 안되길 기대

        Target t = new Target();
        Method targetM = m(Target.class, "hi", String.class);
        MethodInvocation mi = invMock(t, targetM, "yo");
        when(mi.proceed()).thenReturn("hi yo");

        Advice advice = new SimpleAroundInterceptor(sup, adviceM);
        Object out = advice.invoke(mi);
        assertEquals("hi yo", out);

        // invoke 내부에서 static 분기를 넣었으니 이제 통과
        verifyNoInteractions(sup);
    }

    @Test
    @DisplayName("잘못된 시그니처는 IllegalArgumentException")
    void ctor_wrongSignature_illegal() throws Exception {
        class BadAspect {
            void zero() {}                                        // 0 param
            void two(ProceedingJoinPoint p, String x) {}          // 2 params
            void wrongType(String x) {}                           // 1 param but not PJP
        }
        Method zero      = m(BadAspect.class, "zero");
        Method two       = m(BadAspect.class, "two", ProceedingJoinPoint.class, String.class);
        Method wrongType = m(BadAspect.class, "wrongType", String.class);

        assertThrows(IllegalArgumentException.class, () -> new SimpleAroundInterceptor(BadAspect::new, zero));
        assertThrows(IllegalArgumentException.class, () -> new SimpleAroundInterceptor(BadAspect::new, two));
        assertThrows(IllegalArgumentException.class, () -> new SimpleAroundInterceptor(BadAspect::new, wrongType));
    }
}
