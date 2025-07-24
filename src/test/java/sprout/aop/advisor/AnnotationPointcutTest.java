package sprout.aop.advisor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationPointcutTest {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Tx {}

    interface IFace {
        void ifaceMethod();
    }

    @Tx
    static class IFaceImpl implements IFace {
        @Override public void ifaceMethod() {}
    }

    static class Parent {
        @Tx public void parentAnnotated() {}
        public void parentPlain() {}
    }

    static class Child extends Parent {
        @Override public void parentPlain() {}
        public void childPlain() {}
    }

    static class OnlyMethod {
        @Tx void marked() {}
        void unmarked(@Tx String [] arr) {} // 파라미터에만 붙은 케이스(무시)
    }

    static class Plain {
        void plain() {}
    }

    private static Method m(Class<?> c, String name, Class<?>... types) {
        try { return c.getDeclaredMethod(name, types); }
        catch (Exception e) { throw new AssertionError(e); }
    }

    @Test
    @DisplayName("메서드에 직접 붙어 있으면 true")
    void methodAnnotationWins() {
        var pc = new AnnotationPointcut(Tx.class);
        assertTrue(pc.matches(OnlyMethod.class, m(OnlyMethod.class, "marked")));
    }

    @Test
    @DisplayName("메서드엔 없지만 declaring class(부모 포함)에 붙어 있으면 true")
    void declaringClassAnnotationMatches() {
        var pc = new AnnotationPointcut(Tx.class);

        // parentAnnotated() 자체가 부모에 선언, Child로 호출해도 true
        assertTrue(pc.matches(Child.class, m(Parent.class, "parentAnnotated")));
    }

    @Test
    @DisplayName("targetClass 에만 붙어 있어도 true (인터페이스 구현체 등)")
    void targetClassAnnotationMatches() throws NoSuchMethodException {
        var pc = new AnnotationPointcut(Tx.class);
        Method ifaceM = IFace.class.getMethod("ifaceMethod");
        assertTrue(pc.matches(IFaceImpl.class, ifaceM));
    }

    @Test
    @DisplayName("아무 곳에도 없으면 false")
    void noAnnotationAnywhere() {
        var pc = new AnnotationPointcut(Tx.class);
        assertFalse(pc.matches(Plain.class, m(Plain.class, "plain"))); // Object 메서드 예시
    }

    @Test
    @DisplayName("파라미터에만 붙은 경우는 무시한다")
    void parameterAnnotationIgnored() {
        var pc = new AnnotationPointcut(Tx.class);
        assertFalse(pc.matches(OnlyMethod.class, m(OnlyMethod.class, "unmarked", String[].class)));
    }
}
