package sprout.context.builtins;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition;
import sprout.beans.MethodBeanDefinition;
import sprout.beans.annotation.Primary;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class PrimaryResolutionTest {

    interface Foo {}
    @Primary static class FooPrimary implements Foo { public FooPrimary() {} }
    static class FooSecond implements Foo { public FooSecond() {} }

    static class Cfg {
        public Cfg() {}
        @Primary public Bar barPrimary() { return new Bar(); }
        public Bar barSecond() { return new Bar(); }
    }
    static class Bar { public Bar() {} }

    static class Clash1 implements Foo { public Clash1() {} }
    @Primary static class Clash2 implements Foo { public Clash2() {} }
    @Primary static class Clash3 implements Foo { public Clash3() {} }

    private ConstructorBeanDefinition ctorDef(String name, Class<?> type) throws NoSuchMethodException {
        Constructor<?> c = type.getDeclaredConstructors()[0];
        ConstructorBeanDefinition d = new ConstructorBeanDefinition(name, type, c, c.getParameterTypes());
        if (type.isAnnotationPresent(Primary.class)) d.setPrimary(true);
        return d;
    }

    private MethodBeanDefinition factoryDef(String name, Class<?> ret, Class<?> cfg, String cfgName, String method, Class<?>... params) throws NoSuchMethodException {
        Method m = cfg.getDeclaredMethod(method, params);
        MethodBeanDefinition d = new MethodBeanDefinition(name, ret, m, cfgName, m.getParameterTypes());
        if (m.isAnnotationPresent(Primary.class)) d.setPrimary(true);
        return d;
    }

    @Test
    @DisplayName("@Primary 클래스가 선택된다")
    void primaryClassChosen() throws Exception {
        DefaultListableBeanFactory f = new DefaultListableBeanFactory();
        BeanDefinition p = ctorDef("fooPrimary", FooPrimary.class);
        BeanDefinition s = ctorDef("fooSecond", FooSecond.class);
        f.registerBeanDefinition("fooPrimary", p);
        f.registerBeanDefinition("fooSecond", s);

        Foo foo = f.getBean(Foo.class);
        assertInstanceOf(FooPrimary.class, foo);
    }

    @Test
    @DisplayName("@Primary @Bean 메서드가 선택된다")
    void primaryMethodChosen() throws Exception {
        DefaultListableBeanFactory f = new DefaultListableBeanFactory();
        BeanDefinition cfg = ctorDef("cfg", Cfg.class);
        BeanDefinition bar1 = factoryDef("barPrimary", Bar.class, Cfg.class, "cfg", "barPrimary");
        BeanDefinition bar2 = factoryDef("barSecond", Bar.class, Cfg.class, "cfg", "barSecond");

        f.registerBeanDefinition("cfg", cfg);
        f.registerBeanDefinition("barPrimary", bar1);
        f.registerBeanDefinition("barSecond", bar2);

        Bar bar = f.getBean(Bar.class);
        assertSame(f.getBean("barPrimary"), bar);
    }

    @Test
    @DisplayName("@Primary가 2개면 예외")
    void primaryConflict() throws Exception {
        DefaultListableBeanFactory f = new DefaultListableBeanFactory();
        BeanDefinition c1 = ctorDef("c1", Clash1.class);
        BeanDefinition c2 = ctorDef("c2", Clash2.class);
        BeanDefinition c3 = ctorDef("c3", Clash3.class);

        f.registerBeanDefinition("c1", c1);
        f.registerBeanDefinition("c2", c2);
        f.registerBeanDefinition("c3", c3);

        assertThrows(RuntimeException.class, () -> f.getBean(Foo.class));
    }
}
