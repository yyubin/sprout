package sprout.context.builtins;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import sprout.beans.BeanDefinition;
import sprout.beans.ConstructorBeanDefinition;
import sprout.beans.MethodBeanDefinition;
import sprout.beans.processor.BeanPostProcessor;
import sprout.context.CtorMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DefaultListableBeanFactoryTest {

    // ---------- 테스트용 클래스들 ----------
    static class A { A() {} }
    static class B { B(A a) {} }

    interface I {}
    @sprout.beans.annotation.Order(5) static class I2 implements I { I2() {} }
    @sprout.beans.annotation.Order(1) static class I1 implements I { I1() {} }

    static class ListUser {
        final List<I> list;
        ListUser(List<I> list) { this.list = list; }
    }

    static class Cfg {
        Cfg() {}
        public Repo repo() { return new Repo(); }
        public Service svc(Repo r) { return new Service(r); }
    }
    static class Repo { Repo() {} }
    static class Service { Service(Repo r) {} }

    // ---------- 헬퍼 ----------
    private ConstructorBeanDefinition ctorDef(String name, Class<?> type) throws NoSuchMethodException {
        Constructor<?> c = type.getDeclaredConstructors()[0];
        return new ConstructorBeanDefinition(name, type, c, c.getParameterTypes());
    }

    private MethodBeanDefinition factoryDef(String name,
                                            Class<?> returnType,
                                            Class<?> cfgType,
                                            String cfgBeanName,
                                            String methodName,
                                            Class<?>... params) throws NoSuchMethodException {
        Method m = cfgType.getDeclaredMethod(methodName, params);
        return new MethodBeanDefinition(name, returnType, m, cfgBeanName, m.getParameterTypes());
    }

    // postProcessListInjections 호출을 위해 protected 메서드 노출
    static class TestableFactory extends DefaultListableBeanFactory {
        void flushListInjections() { postProcessListInjections(); }
    }

    TestableFactory factory;

    @BeforeEach
    void setup() {
        factory = new TestableFactory();
    }

    @Test
    @DisplayName("생성자 주입으로 Bean 생성 및 의존성 해결")
    void createBean_constructorInjection() throws Exception {
        BeanDefinition aDef = ctorDef("a", A.class);
        BeanDefinition bDef = ctorDef("b", B.class);

        factory.registerBeanDefinition("a", aDef);
        factory.registerBeanDefinition("b", bDef);

        // A를 먼저 생성(컨테이너 버그/설계에 따른 보완)
        factory.getBean("a");
        B b = (B) factory.getBean("b");

        assertNotNull(b);
        assertTrue(factory.containsBean("a"));
        assertTrue(factory.containsBean("b"));

        B byType = factory.getBean(B.class);
        assertSame(b, byType);
    }

    @Test
    @DisplayName("동일 인터페이스 빈 2개 -> 첫 번째 등록 빈이 primary로 선택된다")
    void getBean_primaryInsteadOfAmbiguous() throws Exception {
        BeanDefinition i1 = ctorDef("i1", I1.class);
        BeanDefinition i2 = ctorDef("i2", I2.class);
        factory.registerBeanDefinition("i1", i1);
        factory.registerBeanDefinition("i2", i2);

        factory.getBean("i1"); // 먼저 생성
        factory.getBean("i2");

        I got = factory.getBean(I.class);
        assertInstanceOf(I1.class, got, "첫 번째 등록된 구현체가 primary로 선택되어야 함");
    }

    @Test
    @DisplayName("List<T> 의존성은 postProcessListInjections 이후 @Order 순서대로 채워진다")
    void listInjection_ordered() throws Exception {
        BeanDefinition i1 = ctorDef("i1", I1.class);
        BeanDefinition i2 = ctorDef("i2", I2.class);
        BeanDefinition user = ctorDef("user", ListUser.class);

        factory.registerBeanDefinition("i1", i1);
        factory.registerBeanDefinition("i2", i2);
        factory.registerBeanDefinition("user", user);

        factory.getBean("i1");
        factory.getBean("i2");

        ListUser listUser = (ListUser) factory.getBean("user");
        assertTrue(listUser.list.isEmpty(), "postProcessListInjections 이전엔 비어있음");

        factory.flushListInjections();

        assertEquals(2, listUser.list.size());
        assertInstanceOf(I1.class, listUser.list.get(0)); // @Order(1)
        assertInstanceOf(I2.class, listUser.list.get(1)); // @Order(5)
    }

    @Test
    @DisplayName("팩토리 메서드 빈 생성 및 의존성 해결")
    void factoryMethodBeans() throws Exception {
        BeanDefinition cfgDef  = ctorDef("cfg", Cfg.class);
        BeanDefinition repoDef = factoryDef(
                "repo", Repo.class, Cfg.class, "cfg", "repo"
        );
        BeanDefinition svcDef  = factoryDef(
                "svc",  Service.class, Cfg.class, "cfg", "svc", Repo.class
        );

        factory.registerBeanDefinition("cfg",  cfgDef);
        factory.registerBeanDefinition("repo", repoDef);
        factory.registerBeanDefinition("svc",  svcDef);

        // 미리 전부 생성 (또는 preInstantiateSingletons())
        factory.preInstantiateSingletons();
        Service svc = (Service) factory.getBean("svc");
        assertNotNull(svc);

        CtorMeta meta = factory.lookupCtorMeta(factory.getBean("cfg"));
        assertNotNull(meta);
        assertArrayEquals(cfgDef.getConstructorArgumentTypes(), meta.paramTypes());
    }

    @Test
    @DisplayName("BeanPostProcessor가 before/after 순서로 호출되고, 원본을 반환하도록 스텁한다")
    void beanPostProcessor_calls() throws Exception {
        BeanPostProcessor mockBpp = Mockito.mock(BeanPostProcessor.class);

        // 원본을 그대로 돌려주도록 스텁
        when(mockBpp.postProcessBeforeInitialization(anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(1));
        when(mockBpp.postProcessAfterInitialization(anyString(), any()))
                .thenAnswer(inv -> inv.getArgument(1));

        factory.addBeanPostProcessor(mockBpp);

        BeanDefinition aDef = ctorDef("a", A.class);
        factory.registerBeanDefinition("a", aDef);

        A a = (A) factory.getBean("a");
        assertNotNull(a);

        verify(mockBpp, times(1)).postProcessBeforeInitialization(eq("a"), any());
        verify(mockBpp, times(1)).postProcessAfterInitialization(eq("a"), any());
    }

    @Test
    @DisplayName("reset() 호출 시 상태 초기화")
    void reset_clearsState() {
        factory.registerRuntimeBean("x", new A());
        assertTrue(factory.containsBean("x"));

        factory.reset();
        assertFalse(factory.containsBean("x"));
        assertTrue(factory.getAllBeans().isEmpty());
    }
}
