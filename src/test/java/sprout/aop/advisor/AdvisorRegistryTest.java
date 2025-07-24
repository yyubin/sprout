package sprout.aop.advisor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdvisorRegistryTest {

    static class Target {
        void foo() {}
        void bar() {}
    }

    private Method m(String name) {
        try {
            return Target.class.getDeclaredMethod(name);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    @DisplayName("registerAdvisor 시 order 기준으로 정렬된다")
    void register_sortsByOrder() {
        AdvisorRegistry reg = new AdvisorRegistry();

        Advisor a10 = mock(Advisor.class);
        when(a10.getOrder()).thenReturn(10);

        Advisor a1 = mock(Advisor.class);
        when(a1.getOrder()).thenReturn(1);

        reg.registerAdvisor(a10);
        reg.registerAdvisor(a1);

        assertEquals(List.of(a1, a10), reg.getAllAdvisors());
    }

    @Test
    @DisplayName("getApplicableAdvisors는 pointcut 매칭되는 것만 반환하고 정렬된 상태를 유지한다")
    void applicable_filtersAndKeepsOrder() {
        AdvisorRegistry reg = new AdvisorRegistry();

        Pointcut match = mock(Pointcut.class);
        Pointcut noMatch = mock(Pointcut.class);
        when(match.matches(any(), any())).thenReturn(true);
        when(noMatch.matches(any(), any())).thenReturn(false);

        Advisor a5 = mock(Advisor.class);
        when(a5.getOrder()).thenReturn(5);
        when(a5.getPointcut()).thenReturn(match);

        Advisor a1 = mock(Advisor.class);
        when(a1.getOrder()).thenReturn(1);
        when(a1.getPointcut()).thenReturn(noMatch);

        Advisor a2 = mock(Advisor.class);
        when(a2.getOrder()).thenReturn(2);
        when(a2.getPointcut()).thenReturn(match);

        reg.registerAdvisor(a5);
        reg.registerAdvisor(a1);
        reg.registerAdvisor(a2);

        List<Advisor> result = reg.getApplicableAdvisors(Target.class, m("foo"));
        // a1 제외, a2(2) -> a5(5)
        assertEquals(List.of(a2, a5), result);
    }

    @Test
    @DisplayName("같은 Method로 두 번 조회하면 캐시가 사용되어 Pointcut#matches가 다시 호출되지 않는다")
    void cache_hit_skipsSecondEvaluation() {
        AdvisorRegistry reg = new AdvisorRegistry();

        Pointcut match = mock(Pointcut.class);
        when(match.matches(any(), any())).thenReturn(true);

        Advisor adv = mock(Advisor.class);
        when(adv.getOrder()).thenReturn(1);
        when(adv.getPointcut()).thenReturn(match);

        reg.registerAdvisor(adv);

        Method foo = m("foo");

        // 1st call -> evaluates
        reg.getApplicableAdvisors(Target.class, foo);
        // 2nd call -> should use cache
        reg.getApplicableAdvisors(Target.class, foo);

        verify(match, times(1)).matches(Target.class, foo);
    }

    @Test
    @DisplayName("새 Advisor 등록 시 캐시가 clear 되어 다시 계산된다")
    void cache_cleared_onRegister() {
        AdvisorRegistry reg = new AdvisorRegistry();

        Pointcut p1 = mock(Pointcut.class);
        when(p1.matches(any(), any())).thenReturn(true);
        Advisor a1 = mock(Advisor.class);
        when(a1.getOrder()).thenReturn(1);
        when(a1.getPointcut()).thenReturn(p1);

        reg.registerAdvisor(a1);

        Method foo = m("foo");

        // warm cache
        reg.getApplicableAdvisors(Target.class, foo);
        verify(p1, times(1)).matches(Target.class, foo);

        // 새 advisor 등록 -> cache clear
        Pointcut p2 = mock(Pointcut.class);
        when(p2.matches(any(), any())).thenReturn(true);
        Advisor a2 = mock(Advisor.class);
        when(a2.getOrder()).thenReturn(2);
        when(a2.getPointcut()).thenReturn(p2);

        reg.registerAdvisor(a2);

        // 다시 호출하면 p1, p2 모두 한번씩 호출됨
        reg.getApplicableAdvisors(Target.class, foo);
        verify(p1, times(2)).matches(Target.class, foo);
        verify(p2, times(1)).matches(Target.class, foo);
    }

    @Test
    @DisplayName("getAllAdvisors()는 수정 불가 리스트를 반환한다")
    void allAdvisors_isUnmodifiable() {
        AdvisorRegistry reg = new AdvisorRegistry();
        Advisor a = mock(Advisor.class);
        reg.registerAdvisor(a);

        List<Advisor> list = reg.getAllAdvisors();
        assertThrows(UnsupportedOperationException.class, () -> list.add(a));
    }

    @Test
    @DisplayName("어드바이저가 하나도 없으면 빈 리스트 반환")
    void noAdvisors_returnsEmpty() {
        AdvisorRegistry reg = new AdvisorRegistry();
        assertTrue(reg.getApplicableAdvisors(Target.class, m("bar")).isEmpty());
    }
}
