package sprout.aop;

import net.sf.cglib.proxy.MethodProxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.Pointcut;
import sprout.aop.advice.Advice;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MethodInvocationImplTest {

    // ---- 테스트용 타겟 ----
    static class Target {
        final List<String> log;
        Target(List<String> log) { this.log = log; }

        String foo(String x) {
            log.add("target");
            return "ret:" + x;
        }
    }

    /** 간단히 Method 얻는 헬퍼 */
    private static Method method(String name, Class<?>... types) {
        try { return Target.class.getDeclaredMethod(name, types); }
        catch (NoSuchMethodException e) { throw new AssertionError(e); }
    }

    /** Advisor 생성 헬퍼 (Pointcut은 안 쓰므로 넘겨버림) */
    private static Advisor advisor(Advice advice, int order) {
        return new Advisor() {
            @Override public Pointcut getPointcut() { return null; }
            @Override public Advice getAdvice() { return advice; }
            @Override public int getOrder() { return order; }
        };
    }

    @Test
    @DisplayName("어드바이저 체인이 순서대로 실행되고 마지막에 타겟 메서드가 호출된다")
    void proceed_runsAdvisors_thenTarget() throws Throwable {
        List<String> log = new ArrayList<>();
        Target target = new Target(log);

        Method m = method("foo", String.class);

        // MethodProxy mocking: 실제 메서드로 위임
        MethodProxy proxy = mock(MethodProxy.class);
        when(proxy.invoke(any(), any())).thenAnswer(inv -> m.invoke(inv.getArgument(0), (Object[]) inv.getArgument(1)));

        // Advice 1
        Advice a1 = inv -> {
            log.add("a1_before");
            Object r = inv.proceed();
            log.add("a1_after");
            return r;
        };

        // Advice 2
        Advice a2 = inv -> {
            log.add("a2_before");
            Object r = inv.proceed();
            log.add("a2_after");
            return r;
        };

        List<Advisor> advisors = List.of(
                advisor(a1, 1),
                advisor(a2, 2)
        );

        MethodInvocationImpl invocation =
                new MethodInvocationImpl(target, m, new Object[]{"X"}, proxy, advisors);

        Object result = invocation.proceed();

        assertEquals("ret:X", result);
        assertEquals(List.of("a1_before", "a2_before", "target", "a2_after", "a1_after"), log);
        verify(proxy, times(1)).invoke(target, new Object[]{"X"});
    }

    @Test
    @DisplayName("어드바이저가 없으면 바로 타겟 메서드가 호출된다")
    void noAdvisors_callsTargetDirectly() throws Throwable {
        List<String> log = new ArrayList<>();
        Target target = new Target(log);
        Method m = method("foo", String.class);

        MethodProxy proxy = mock(MethodProxy.class);
        when(proxy.invoke(any(), any())).thenAnswer(inv -> m.invoke(inv.getArgument(0), (Object[]) inv.getArgument(1)));

        MethodInvocationImpl invocation =
                new MethodInvocationImpl(target, m, new Object[]{"Y"}, proxy, List.of());

        Object result = invocation.proceed();

        assertEquals("ret:Y", result);
        assertEquals(List.of("target"), log);
        verify(proxy, times(1)).invoke(target, new Object[]{"Y"});
    }

    @Test
    @DisplayName("어드바이스가 proceed()를 호출하지 않으면 체인이 중단되고 타겟은 호출되지 않는다 (short-circuit)")
    void advice_canShortCircuit() throws Throwable {
        List<String> log = new ArrayList<>();
        Target target = new Target(log);
        Method m = method("foo", String.class);

        MethodProxy proxy = mock(MethodProxy.class);

        Advice shortCircuit = inv -> {
            log.add("short");
            return "shorted";
        };

        List<Advisor> advisors = List.of(advisor(shortCircuit, 1));

        MethodInvocationImpl invocation =
                new MethodInvocationImpl(target, m, new Object[]{"Z"}, proxy, advisors);

        Object result = invocation.proceed();

        assertEquals("shorted", result);
        assertEquals(List.of("short"), log);
        verify(proxy, times(0)).invoke(any(), any()); // 타겟 호출 안 됨
    }

    @Test
    @DisplayName("getSignature/getArgs/getTarget 등 단순 getter 동작 확인")
    void getters_work() throws Throwable {
        Target target = new Target(new ArrayList<>());
        Method m = method("foo", String.class);

        MethodProxy proxy = mock(MethodProxy.class);
        List<Advisor> advisors = List.of();

        MethodInvocationImpl invocation =
                new MethodInvocationImpl(target, m, new Object[]{"A"}, proxy, advisors);

        assertEquals(target, invocation.getTarget());
        assertArrayEquals(new Object[]{"A"}, invocation.getArgs());
        assertArrayEquals(new Object[]{"A"}, invocation.getArguments());
        assertEquals(m, invocation.getMethod());
        assertEquals("foo", invocation.getSignature().getName());
    }
}
