package sprout.aop.advisor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CompositePointcutTest {

    static class Dummy {
        void foo() {}
    }

    private static Method m(String name, Class<?>... types) {
        try { return Dummy.class.getDeclaredMethod(name, types); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    @Test
    @DisplayName("하나라도 true면 전체가 true (OR)")
    void anyTrue_returnsTrue() {
        Pointcut f = (c, me) -> false;
        Pointcut t = (c, me) -> true;

        CompositePointcut pc = new CompositePointcut(f, t);
        assertTrue(pc.matches(Dummy.class, m("foo")));
    }

    @Test
    @DisplayName("모두 false면 false")
    void allFalse_returnsFalse() {
        Pointcut f1 = (c, me) -> false;
        Pointcut f2 = (c, me) -> false;

        CompositePointcut pc = new CompositePointcut(f1, f2);
        assertFalse(pc.matches(Dummy.class, m("foo")));
    }

    @Test
    @DisplayName("빈 리스트면 false")
    void empty_returnsFalse() {
        CompositePointcut pc = new CompositePointcut(List.of());
        assertFalse(pc.matches(Dummy.class, m("foo")));
    }

    @Test
    @DisplayName("앞에서 true면 뒤는 호출되지 않는다(쇼트서킷)")
    void shortCircuit_whenFirstTrue() {
        AtomicInteger count = new AtomicInteger(0);

        Pointcut first = (c, me) -> { count.incrementAndGet(); return true; };
        Pointcut second = (c, me) -> { count.incrementAndGet(); return true; };

        CompositePointcut pc = new CompositePointcut(first, second);
        assertTrue(pc.matches(Dummy.class, m("foo")));
        assertEquals(1, count.get(), "두 번째 Pointcut은 호출되면 안 됨");
    }
}
