package sprout.aop;

import net.sf.cglib.proxy.Enhancer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sprout.aop.advice.Advice;
import sprout.aop.advice.AdviceFactory;
import sprout.aop.advisor.*;
import sprout.aop.annotation.Aspect;
import sprout.beans.processor.BeanPostProcessor;
import sprout.context.ApplicationContext;
import sprout.context.CtorMeta;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AspectPostProcessorTest {

    @Aspect
    static class MyAspect {
        public void a() {}
        public void b() {}
    }

    static class Target {
        final List<String> log = new ArrayList<>();
        public String foo(String x) { log.add("target"); return "ret:" + x; }
        public void bar() { log.add("bar"); }
        public Target() {}
    }

    static class StubProxyFactory implements ProxyFactory {
        @Override
        public Object createProxy(Class<?> targetClass,
                                  Object target,
                                  AdvisorRegistry registry,
                                  CtorMeta meta) {
            return target;
        }
    }

    // ---------- helpers ----------
    private static Advisor advisor(Advice advice, int order) {
        return new Advisor() {
            @Override public Pointcut getPointcut() { return null; }
            @Override public Advice getAdvice() { return advice; }
            @Override public int getOrder() { return order; }
        };
    }

    private static Method m(Class<?> c, String name, Class<?>... types) {
        try { return c.getDeclaredMethod(name, types); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    // ---------- tests ----------

    @Test
    @DisplayName("initialize: @Aspect 클래스의 메서드마다 Advisor를 만들고 레지스트리에 등록, 두 번째 호출은 무시")
    void initialize_registersOnce() {
        AdvisorRegistry registry = mock(AdvisorRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        AdviceFactory factory = mock(AdviceFactory.class);
        ProxyFactory pf = mock(StubProxyFactory.class);

        when(ctx.getBean(MyAspect.class)).thenReturn(new MyAspect());

        // createAdvisor -> 항상 advisor 하나 반환
        when(factory.createAdvisor(eq(MyAspect.class), any(Method.class), any()))
                .thenAnswer(inv -> Optional.of(mock(Advisor.class)));

        AspectPostProcessor pp = new AspectPostProcessor(registry, ctx, factory, pf);

        String pkg = MyAspect.class.getPackageName();
        pp.initialize(List.of(pkg));

        // MyAspect의 declaredMethods 개수만큼 호출
        verify(factory, atLeast(1)).createAdvisor(eq(MyAspect.class), any(Method.class), any());
        verify(registry, atLeast(1)).registerAdvisor(any());

        // 두 번째 initialize는 무시
        reset(factory, registry);
        pp.initialize(List.of(pkg));
        verifyNoInteractions(factory, registry);
    }

    @Test
    @DisplayName("Advisor 없으면 프록시 생성 없이 원본 bean 반환")
    void postProcess_noAdvisor_returnsOriginal() {
        AdvisorRegistry registry = mock(AdvisorRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        AdviceFactory factory = mock(AdviceFactory.class);
        ProxyFactory pf = mock(StubProxyFactory.class);

        AspectPostProcessor pp = new AspectPostProcessor(registry, ctx, factory, pf);

        Target bean = new Target();

        // 어떤 메서드도 advisor 없음
        when(registry.getApplicableAdvisors(eq(Target.class), any(Method.class))).thenReturn(List.of());

        // CtorMeta mock
        CtorMeta meta = mock(CtorMeta.class);
        when(meta.paramTypes()).thenReturn(new Class<?>[0]);
        when(meta.args()).thenReturn(new Object[0]);
        when(ctx.lookupCtorMeta(bean)).thenReturn(meta);

        Object processed = pp.postProcessAfterInitialization("target", bean);
        assertSame(bean, processed);
    }

    @Test
    void postProcess_withAdvisor_callsProxyFactory() throws NoSuchMethodException {
        AdvisorRegistry reg = mock(AdvisorRegistry.class);
        ApplicationContext ctx = mock(ApplicationContext.class);
        AdviceFactory factory = mock(AdviceFactory.class);
        ProxyFactory pf = mock(StubProxyFactory.class);

        AspectPostProcessor pp = new AspectPostProcessor(reg, ctx, factory, pf);

        Target bean = new Target();
        Method foo = Target.class.getMethod("foo", String.class);

        when(reg.getApplicableAdvisors(eq(Target.class), any(Method.class)))
                .thenReturn(List.of());                 // default
        when(reg.getApplicableAdvisors(Target.class, foo))
                .thenReturn(List.of(mock(Advisor.class))); // foo만 매칭된다고 가정

        CtorMeta meta = mock(CtorMeta.class);
        when(meta.paramTypes()).thenReturn(new Class<?>[0]);
        when(meta.args()).thenReturn(new Object[0]);
        when(ctx.lookupCtorMeta(bean)).thenReturn(meta);

        Object ret = pp.postProcessAfterInitialization("target", bean);

        verify(pf).createProxy(Target.class, bean, reg, meta);
    }

}
