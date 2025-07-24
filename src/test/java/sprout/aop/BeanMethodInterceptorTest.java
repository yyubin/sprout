package sprout.aop;

import net.sf.cglib.proxy.MethodProxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.aop.advice.Advice;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.AdvisorRegistry;
import sprout.aop.advisor.Pointcut;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BeanMethodInterceptorTest {

    static class Target {
        final List<String> log = new ArrayList<>();
        String foo(String x) { log.add("target"); return "ret:" + x; }
        void boom() { throw new IllegalStateException("boom"); }
    }

    private static Method m(Class<?> c, String name, Class<?>... types) {
        try { return c.getDeclaredMethod(name, types); }
        catch (NoSuchMethodException e) { throw new AssertionError(e); }
    }

    private static Advisor advisor(Advice advice, int order) {
        return new Advisor() {
            @Override public Pointcut getPointcut() { return null; }
            @Override public Advice getAdvice() { return advice; }
            @Override public int getOrder() { return order; }
        };
    }

    @Test
    @DisplayName("어드바이저가 없으면 proxy.invoke로 바로 타겟 호출")
    void noAdvisor_callsTargetDirectly() throws Throwable {
        Target target = new Target();
        AdvisorRegistry registry = mock(AdvisorRegistry.class);
        MethodProxy proxy = mock(MethodProxy.class);

        Method foo = m(Target.class, "foo", String.class);

        when(registry.getApplicableAdvisors(Target.class, foo)).thenReturn(List.of());
        when(proxy.invoke(target, new Object[]{"X"})).thenAnswer(inv ->
                foo.invoke(target, (Object[]) inv.getArgument(1))
        );

        BeanMethodInterceptor interceptor = new BeanMethodInterceptor(target, registry);

        Object result = interceptor.intercept(target, foo, new Object[]{"X"}, proxy);

        assertEquals("ret:X", result);
        assertEquals(List.of("target"), target.log);
        verify(proxy, times(1)).invoke(target, new Object[]{"X"});
        verify(registry).getApplicableAdvisors(Target.class, foo);
    }

    @Test
    @DisplayName("어드바이저가 있으면 Advice 체인 실행 후 타겟 호출")
    void advisors_chainRuns() throws Throwable {
        Target target = new Target();
        AdvisorRegistry registry = mock(AdvisorRegistry.class);
        MethodProxy proxy = mock(MethodProxy.class);
        Method foo = m(Target.class, "foo", String.class);

        when(proxy.invoke(any(), any())).thenAnswer(inv ->
                foo.invoke(inv.getArgument(0), (Object[]) inv.getArgument(1))
        );

        Advice a1 = inv -> {
            target.log.add("a1_before");
            Object r = inv.proceed();
            target.log.add("a1_after");
            return r;
        };
        Advice a2 = inv -> {
            target.log.add("a2_before");
            Object r = inv.proceed();
            target.log.add("a2_after");
            return r;
        };

        List<Advisor> advisors = List.of(advisor(a1, 1), advisor(a2, 2));
        when(registry.getApplicableAdvisors(Target.class, foo)).thenReturn(advisors);

        BeanMethodInterceptor interceptor = new BeanMethodInterceptor(target, registry);

        Object result = interceptor.intercept(target, foo, new Object[]{"X"}, proxy);

        assertEquals("ret:X", result);
        assertEquals(List.of("a1_before","a2_before","target","a2_after","a1_after"), target.log);
        verify(proxy, times(1)).invoke(target, new Object[]{"X"});
    }

    @Test
    @DisplayName("Advice가 proceed()를 호출하지 않으면 타겟 미호출 (short-circuit)")
    void shortCircuit_adviceSkipsTarget() throws Throwable {
        Target target = new Target();
        AdvisorRegistry registry = mock(AdvisorRegistry.class);
        MethodProxy proxy = mock(MethodProxy.class);
        Method foo = m(Target.class, "foo", String.class);

        Advice shortA = inv -> { target.log.add("short"); return "shorted"; };
        when(registry.getApplicableAdvisors(Target.class, foo))
                .thenReturn(List.of(advisor(shortA, 1)));

        BeanMethodInterceptor interceptor = new BeanMethodInterceptor(target, registry);

        Object result = interceptor.intercept(target, foo, new Object[]{"Y"}, proxy);

        assertEquals("shorted", result);
        assertEquals(List.of("short"), target.log);
        verify(proxy, never()).invoke(any(), any());
    }

    @Test
    @DisplayName("타겟 메서드 예외는 그대로 전파된다")
    void exceptionPropagates() throws Throwable {
        Target target = new Target();
        AdvisorRegistry registry = mock(AdvisorRegistry.class);
        MethodProxy proxy = mock(MethodProxy.class);
        Method boom = m(Target.class, "boom");

        when(registry.getApplicableAdvisors(Target.class, boom)).thenReturn(List.of());
        when(proxy.invoke(any(), any())).thenAnswer(inv -> {
            try {
                return boom.invoke(inv.getArgument(0), (Object[]) inv.getArgument(1));
            } catch (InvocationTargetException e) {
                throw e.getTargetException();   // <- 원래 예외 그대로 던짐
            }
        });

        BeanMethodInterceptor interceptor = new BeanMethodInterceptor(target, registry);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> interceptor.intercept(target, boom, new Object[]{}, proxy));
        assertEquals("boom", ex.getMessage());
    }
}
