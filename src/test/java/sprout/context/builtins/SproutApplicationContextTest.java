package sprout.context.builtins;

import org.junit.jupiter.api.*;
import sprout.beans.annotation.Component;
import sprout.beans.annotation.Primary;
import sprout.beans.processor.BeanPostProcessor;
import sprout.context.*;
import sprout.beans.InfrastructureBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SproutApplicationContextTest {

    static class Flags {
        static boolean postInfraCalled;
        static boolean ctxInitCalled;
        static final List<String> before = new ArrayList<>();
        static final List<String> after = new ArrayList<>();
        static void reset() {
            postInfraCalled = false;
            ctxInitCalled = false;
            before.clear();
            after.clear();
        }
    }

    @Component
    static class Foo {}

    interface Bar {}
    @Component @Primary
    static class BarImpl1 implements Bar {}
    @Component
    static class BarImpl2 implements Bar {}

    @Component
    static class MyBpp implements BeanPostProcessor, InfrastructureBean {
        @Override
        public Object postProcessBeforeInitialization(String name, Object bean) {
            Flags.before.add(name);
            return bean;
        }
        @Override
        public Object postProcessAfterInitialization(String name, Object bean) {
            Flags.after.add(name);
            return bean;
        }
    }

    @Component
    static class MyPostInfra implements PostInfrastructureInitializer, InfrastructureBean {
        @Override
        public void afterInfrastructureSetup(BeanFactory factory, List<String> basePackages) {
            Flags.postInfraCalled = true;
        }
    }

    @Component
    static class MyCtxInit implements ContextInitializer {
        @Override
        public void initializeAfterRefresh(BeanFactory ctx) {
            Flags.ctxInitCalled = true;
        }
    }

    SproutApplicationContext ctx;

    @BeforeEach
    void setUp() {
        Flags.reset();
        ctx = new SproutApplicationContext("sprout.context.builtins");
    }

    @AfterEach
    void tearDown() {
        ctx.close();
    }

    @Test
    @DisplayName("refresh 후 컴포넌트 스캔 및 빈 생성, 초기화 훅 동작")
    void refresh_flow() throws Exception {
        ctx.refresh();

        Foo foo = ctx.getBean(Foo.class);
        assertNotNull(foo);

        Bar bar = ctx.getBean(Bar.class);
        assertInstanceOf(BarImpl1.class, bar);

        assertTrue(Flags.postInfraCalled);
        assertTrue(Flags.ctxInitCalled);

        assertTrue(Flags.before.contains("foo"));
        assertTrue(Flags.after.contains("foo"));
    }

    @Test
    @DisplayName("close/reset 후 빈이 비워진다")
    void close_resets() throws Exception {
        ctx.refresh();
        assertFalse(ctx.getAllBeans().isEmpty());

        ctx.close();
        assertTrue(ctx.getAllBeans().isEmpty());
    }
}
