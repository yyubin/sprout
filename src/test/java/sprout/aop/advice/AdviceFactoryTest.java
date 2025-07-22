package sprout.aop.advice;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import sprout.aop.advisor.Advisor;
import sprout.aop.advisor.PointcutFactory;
import sprout.aop.annotation.After;
import sprout.aop.annotation.Around;
import sprout.aop.annotation.Before;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdviceFactoryTest {

    AdviceFactory factory;
    PointcutFactory pointcutFactory; // 실제 내부 호출은 모킹 처리

    @BeforeEach
    void setUp() {
        pointcutFactory = mock(PointcutFactory.class, RETURNS_DEEP_STUBS);
        factory = new AdviceFactory(pointcutFactory);
    }

    // ---------- 테스트용 Aspect 클래스 ----------
    static class MyAspect {

        @Before
        public void beforeMeth() {}

        @After
        public void afterMeth() {}

        @Around
        public Object aroundMeth(sprout.aop.ProceedingJoinPoint pjp) throws Throwable {
            return pjp.proceed();
        }

        public void noAnno() {}
    }

    private Supplier<Object> supplier() {
        return MyAspect::new;
    }

    private Method m(String name, Class<?>... params) throws NoSuchMethodException {
        return MyAspect.class.getMethod(name, params);
    }

    // ---------- Tests ----------

    @Test
    @DisplayName("@Before 메서드 → Advisor Optional present")
    void beforeAdvisorCreated() throws Exception {
        Optional<Advisor> adv = factory.createAdvisor(MyAspect.class, m("beforeMeth"), supplier());
        assertTrue(adv.isPresent());
    }

    @Test
    @DisplayName("@After 메서드 → Advisor Optional present")
    void afterAdvisorCreated() throws Exception {
        Optional<Advisor> adv = factory.createAdvisor(MyAspect.class, m("afterMeth"), supplier());
        assertTrue(adv.isPresent());
    }

    @Test
    @DisplayName("@Around 메서드 → Advisor Optional present")
    void aroundAdvisorCreated() throws Exception {
        Optional<Advisor> adv = factory.createAdvisor(MyAspect.class,
                m("aroundMeth", sprout.aop.ProceedingJoinPoint.class), supplier());
        assertTrue(adv.isPresent());
    }

    @Test
    @DisplayName("어노테이션 없음 → Optional.empty()")
    void noAnnotation_returnsEmpty() throws Exception {
        Optional<Advisor> adv = factory.createAdvisor(MyAspect.class, m("noAnno"), supplier());
        assertTrue(adv.isEmpty());
    }
}
