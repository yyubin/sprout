package sprout.aop.advisor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AspectJPointcutAdapterTest {

    static class Sample {
        public void greet() {}
        public String calc(int a) { return String.valueOf(a); }
    }

    static class Other {
        public void something() {}
    }

    private static Method m(Class<?> clazz, String name, Class<?>... params) {
        try {
            return clazz.getDeclaredMethod(name, params);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    @DisplayName("alwaysMatches or maybeMatches → true")
    void matches_true_whenExpressionMatches() {
        // execution(* sprout.aop.advisor.AspectJPointcutAdapterTest.Sample.*(..))
        String expr = "execution(* " + Sample.class.getName() + ".*(..))";
        AspectJPointcutAdapter pc = new AspectJPointcutAdapter(expr);

        assertTrue(pc.matches(Sample.class, m(Sample.class, "greet")));
        assertTrue(pc.matches(Sample.class, m(Sample.class, "calc", int.class)));
    }

    @Test
    @DisplayName("couldMatchJoinPointsInType == false → 바로 false")
    void matches_false_whenTypeCannotMatch() {
        // 타입을 아예 다른 패키지로 지정해서 couldMatchJoinPointsInType이 false 나도록
        String expr = "execution(* com.foo..*(..))";
        AspectJPointcutAdapter pc = new AspectJPointcutAdapter(expr);

        assertFalse(pc.matches(Sample.class, m(Sample.class, "greet")));
    }

    @Test
    @DisplayName("같은 타입이지만 메서드는 매칭되지 않으면 neverMatches → false")
    void matches_false_whenMethodDoesNotMatch() {
        // greet()만 매칭, calc()는 매칭 안 됨
        String expr = "execution(* " + Sample.class.getName() + ".greet(..))";
        AspectJPointcutAdapter pc = new AspectJPointcutAdapter(expr);

        assertFalse(pc.matches(Sample.class, m(Sample.class, "calc", int.class)));
    }

    @Test
    @DisplayName("toString()은 표현식 문자열을 포함해야 한다")
    void toString_containsExpression() {
        String expr = "execution(* " + Sample.class.getName() + ".*(..))";
        AspectJPointcutAdapter pc = new AspectJPointcutAdapter(expr);

        String s = pc.toString();
        assertTrue(s.contains(expr), "toString() should contain original expression");
    }
}
